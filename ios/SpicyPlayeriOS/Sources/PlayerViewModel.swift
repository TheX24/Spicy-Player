import AVFoundation
import SwiftUI
import UIKit
import UniformTypeIdentifiers

@MainActor
final class PlayerViewModel: ObservableObject {
    @Published var lines: [LyricLine] = []
    @Published var currentTimeMs = 0
    @Published var isPlaying = false
    @Published var artwork: UIImage?
    @Published var nowPlayingTitle = "No Track Loaded"
    @Published var lyricsStatus = "Import a song or a folder to start."
    @Published var errorMessage: String?
    @Published var libraryTracks: [ImportedTrack] = []
    @Published var selectedTrackID: ImportedTrack.ID?
    @Published var lyricOffsetMs = UserDefaults.standard.integer(forKey: "SpicyPlayeriOS.lyricOffsetMs")
    @Published var isShuffleEnabled = UserDefaults.standard.bool(forKey: "SpicyPlayeriOS.shuffleEnabled")

    private let player = AVPlayer()
    private var timeObserver: Any?
    private var currentTrackBaseName: String?
    private let supportedAudioExtensions: Set<String> = ["flac", "mp3", "wav", "m4a", "aac", "alac", "caf", "aif", "aiff"]
    private let lastSelectedTrackDefaultsKey = "SpicyPlayeriOS.lastSelectedTrackBaseName"
    private let lyricOffsetDefaultsKey = "SpicyPlayeriOS.lyricOffsetMs"
    private let shuffleDefaultsKey = "SpicyPlayeriOS.shuffleEnabled"

    init() {
        configureAudioSession()
        installTimeObserver()
        ensureLibraryDirectories()
        refreshLibrary()

        Task {
            await restoreLastSelectedTrack()
        }
    }

    deinit {
        if let timeObserver {
            player.removeTimeObserver(timeObserver)
        }
    }

    func supportedAudioTypes() -> [UTType] {
        var result: [UTType] = [.audio]
        if let flac = UTType(filenameExtension: "flac") {
            result.append(flac)
        }
        return result
    }

    func supportedLyricsTypes() -> [UTType] {
        var result: [UTType] = []
        if let ttml = UTType(filenameExtension: "ttml") {
            result.append(ttml)
        }
        result.append(.xml)
        return result
    }

    func supportedFolderTypes() -> [UTType] {
        [.folder]
    }

    func importSong(from externalURL: URL) async {
        errorMessage = nil

        do {
            let importedAudioURL = try importAudioFile(from: externalURL)
            let baseName = importedAudioURL.deletingPathExtension().lastPathComponent

            try? importSiblingLyricsIfAvailable(for: externalURL, baseName: baseName)
            refreshLibrary()
            await loadTrack(baseName: baseName, autoplay: true)
        } catch {
            errorMessage = "Song import failed: \(error.localizedDescription)"
            lyricsStatus = "Import a song or a folder to start."
        }
    }

    func importFolder(from externalFolderURL: URL) async {
        errorMessage = nil

        do {
            let importedBaseNames = try importFolderContents(from: externalFolderURL)
            refreshLibrary()

            if let firstImportedBaseName = importedBaseNames.first {
                await loadTrack(baseName: firstImportedBaseName, autoplay: false)
                lyricsStatus = "Imported \(importedBaseNames.count) track(s) from folder."
            } else {
                lyricsStatus = "No supported audio files were found in that folder."
            }
        } catch {
            errorMessage = "Folder import failed: \(error.localizedDescription)"
        }
    }

    func importLyricsForCurrentTrack(from externalURL: URL) async {
        errorMessage = nil

        guard let baseName = currentTrackBaseName else {
            errorMessage = "Import a song first, then attach lyrics."
            return
        }

        do {
            let destination = lyricsDirectoryURL().appendingPathComponent("\(baseName).ttml")
            _ = try importFile(from: externalURL, to: destination)
            refreshLibrary()
            lyricsStatus = "Imported lyrics for \(baseName)."
            await loadBestLyricsMatch(for: baseName)
        } catch {
            errorMessage = "Lyrics import failed: \(error.localizedDescription)"
        }
    }

    func loadTrack(_ track: ImportedTrack, autoplay: Bool = false) async {
        await loadTrack(baseName: track.baseName, autoplay: autoplay)
    }

    func canAttachLyrics() -> Bool {
        currentTrackBaseName != nil
    }

    func togglePlayback() {
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    func playPreviousTrack() async {
        guard !libraryTracks.isEmpty else {
            return
        }

        if isShuffleEnabled, libraryTracks.count > 1 {
            await shufflePlay()
            return
        }

        let currentIndex = libraryTracks.firstIndex { $0.id == selectedTrackID } ?? 0
        let previousIndex = currentIndex == 0 ? libraryTracks.index(before: libraryTracks.endIndex) : libraryTracks.index(before: currentIndex)
        await loadTrack(libraryTracks[previousIndex], autoplay: true)
    }

    func playNextTrack() async {
        guard !libraryTracks.isEmpty else {
            return
        }

        if isShuffleEnabled, libraryTracks.count > 1 {
            await shufflePlay()
            return
        }

        let currentIndex = libraryTracks.firstIndex { $0.id == selectedTrackID } ?? 0
        let nextIndex = libraryTracks.index(after: currentIndex) == libraryTracks.endIndex ? libraryTracks.startIndex : libraryTracks.index(after: currentIndex)
        await loadTrack(libraryTracks[nextIndex], autoplay: true)
    }

    func shufflePlay() async {
        guard !libraryTracks.isEmpty else {
            return
        }

        let currentID = selectedTrackID
        let candidates = libraryTracks.filter { $0.id != currentID }
        let selectedTrack = candidates.randomElement() ?? libraryTracks.randomElement()

        if let selectedTrack {
            await loadTrack(selectedTrack, autoplay: true)
        }
    }

    func toggleShuffle() {
        isShuffleEnabled.toggle()
        UserDefaults.standard.set(isShuffleEnabled, forKey: shuffleDefaultsKey)
    }

    func adjustLyricOffset(by deltaMs: Int) {
        lyricOffsetMs = min(max(lyricOffsetMs + deltaMs, -2000), 2000)
        UserDefaults.standard.set(lyricOffsetMs, forKey: lyricOffsetDefaultsKey)
    }

    func resetLyricOffset() {
        lyricOffsetMs = 0
        UserDefaults.standard.set(lyricOffsetMs, forKey: lyricOffsetDefaultsKey)
    }

    func seek(to timeMs: Int) {
        let time = CMTime(seconds: Double(timeMs) / 1000.0, preferredTimescale: 600)
        player.seek(to: time, toleranceBefore: .zero, toleranceAfter: .zero)
        currentTimeMs = timeMs
    }

    func activeLineID() -> UUID? {
        activeLineID(for: currentTimeMs)
    }

    func activeLineID(for timeMs: Int) -> UUID? {
        if let current = lines.first(where: { !$0.isBackground && !$0.isSongwriter && $0.startMs <= timeMs && timeMs <= $0.endMs }) {
            return current.id
        }

        return lines
            .filter { !$0.isBackground && !$0.isSongwriter && $0.startMs <= timeMs }
            .last?
            .id
    }

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func installTimeObserver() {
        let interval = CMTime(seconds: 1.0 / 30.0, preferredTimescale: 600)
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self else {
                return
            }

            Task { @MainActor in
                let milliseconds = Int((time.seconds * 1000.0).rounded())
                if milliseconds >= 0 {
                    self.currentTimeMs = milliseconds
                }

                if let currentItem = self.player.currentItem {
                    let isAtEnd = currentItem.status == .readyToPlay && currentItem.duration.isNumeric && time >= currentItem.duration
                    if isAtEnd {
                        self.isPlaying = false
                    }
                }
            }
        }
    }

    private func replaceCurrentItem(with url: URL, autoplay: Bool) {
        player.pause()
        let item = AVPlayerItem(url: url)
        player.replaceCurrentItem(with: item)
        currentTimeMs = 0

        if autoplay {
            player.play()
            isPlaying = true
        } else {
            isPlaying = false
        }
    }

    private func restoreLastSelectedTrack() async {
        guard let storedBaseName = UserDefaults.standard.string(forKey: lastSelectedTrackDefaultsKey) else {
            return
        }

        await loadTrack(baseName: storedBaseName, autoplay: false)
    }

    private func loadTrack(baseName: String, autoplay: Bool) async {
        refreshLibrary()

        guard let track = libraryTracks.first(where: { compareBaseName($0.baseName, baseName) }) else {
            return
        }

        errorMessage = nil
        lines = []
        artwork = nil
        currentTrackBaseName = track.baseName
        nowPlayingTitle = track.title
        selectedTrackID = track.id
        UserDefaults.standard.set(track.baseName, forKey: lastSelectedTrackDefaultsKey)

        await loadArtwork(from: track.audioURL)
        replaceCurrentItem(with: track.audioURL, autoplay: autoplay)
        await loadBestLyricsMatch(for: track.baseName)
    }

    private func loadBestLyricsMatch(for baseName: String) async {
        guard let lyricsURL = findLyricsURL(for: baseName) else {
            lines = []
            lyricsStatus = "No imported lyrics found. Use Attach Lyrics to pair a .ttml file."
            refreshLibrary()
            return
        }

        await loadLyrics(from: lyricsURL)
        refreshLibrary()
    }

    private func loadLyrics(from url: URL) async {
        do {
            let data = try Data(contentsOf: url)
            let parsed = try TTMLLyricsParser.parse(data: data)
            lines = parsed.lines
            lyricsStatus = parsed.lines.isEmpty
                ? "Imported lyrics file is empty."
                : "Lyrics paired from \(url.lastPathComponent)"
        } catch {
            lines = []
            lyricsStatus = "Imported lyrics could not be parsed."
            errorMessage = "Lyrics could not be parsed: \(error.localizedDescription)"
        }
    }

    private func loadArtwork(from url: URL) async {
        let asset = AVURLAsset(url: url)

        do {
            artwork = try await extractArtwork(from: asset)
        } catch {
            artwork = nil
        }
    }

    private func extractArtwork(from asset: AVURLAsset) async throws -> UIImage? {
        let commonMetadata = try await asset.load(.commonMetadata)
        if let image = try await image(from: commonMetadata) {
            return image
        }

        let metadataFormats = try await asset.load(.availableMetadataFormats)
        for format in metadataFormats {
            let items = asset.metadata(forFormat: format)
            if let image = try await image(from: items) {
                return image
            }
        }

        return nil
    }

    private func image(from items: [AVMetadataItem]) async throws -> UIImage? {
        for item in items {
            let commonKey = item.commonKey?.rawValue.lowercased()
            let identifier = item.identifier?.rawValue.lowercased()
            let looksLikeArtwork = commonKey == "artwork" || identifier?.contains("artwork") == true

            guard looksLikeArtwork else {
                continue
            }

            if let data = try? await item.load(.dataValue), let image = UIImage(data: data) {
                return image
            }
        }

        return nil
    }

    private func refreshLibrary() {
        ensureLibraryDirectories()

        let audioFiles = ((try? FileManager.default.contentsOfDirectory(at: audioDirectoryURL(), includingPropertiesForKeys: nil)) ?? [])
            .filter(isSupportedAudioURL(_:))
            .sorted { $0.lastPathComponent.localizedCaseInsensitiveCompare($1.lastPathComponent) == .orderedAscending }

        libraryTracks = audioFiles.map { audioURL in
            let baseName = audioURL.deletingPathExtension().lastPathComponent
            return ImportedTrack(
                baseName: baseName,
                title: baseName,
                audioURL: audioURL,
                lyricsURL: findLyricsURL(for: baseName)
            )
        }

        if let currentTrackBaseName,
           let matchingTrack = libraryTracks.first(where: { compareBaseName($0.baseName, currentTrackBaseName) }) {
            selectedTrackID = matchingTrack.id
        } else if let existingSelectedTrackID = selectedTrackID,
                  !libraryTracks.contains(where: { $0.id == existingSelectedTrackID }) {
            selectedTrackID = nil
        }
    }

    private func ensureLibraryDirectories() {
        let fileManager = FileManager.default
        try? fileManager.createDirectory(at: audioDirectoryURL(), withIntermediateDirectories: true)
        try? fileManager.createDirectory(at: lyricsDirectoryURL(), withIntermediateDirectories: true)
    }

    private func libraryRootURL() -> URL {
        let applicationSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        return applicationSupport.appendingPathComponent("ImportedMedia", isDirectory: true)
    }

    private func audioDirectoryURL() -> URL {
        libraryRootURL().appendingPathComponent("Audio", isDirectory: true)
    }

    private func lyricsDirectoryURL() -> URL {
        libraryRootURL().appendingPathComponent("Lyrics", isDirectory: true)
    }

    private func findLyricsURL(for baseName: String) -> URL? {
        let preferred = lyricsDirectoryURL().appendingPathComponent("\(baseName).ttml")
        if FileManager.default.fileExists(atPath: preferred.path) {
            return preferred
        }

        let candidates = (try? FileManager.default.contentsOfDirectory(at: lyricsDirectoryURL(), includingPropertiesForKeys: nil)) ?? []
        return candidates.first {
            compareBaseName($0.deletingPathExtension().lastPathComponent, baseName)
        }
    }

    private func importAudioFile(from externalURL: URL) throws -> URL {
        let destination = audioDirectoryURL().appendingPathComponent(sanitizedFilename(externalURL.lastPathComponent))
        return try importFile(from: externalURL, to: destination)
    }

    private func importFolderContents(from externalFolderURL: URL) throws -> [String] {
        ensureLibraryDirectories()

        return try withSecurityScopedAccess(to: externalFolderURL) {
            let fileManager = FileManager.default
            let enumerator = fileManager.enumerator(
                at: externalFolderURL,
                includingPropertiesForKeys: [.isRegularFileKey],
                options: [.skipsHiddenFiles]
            )

            var importedAudioBaseNames: [String] = []

            while let fileURL = enumerator?.nextObject() as? URL {
                let resourceValues = try? fileURL.resourceValues(forKeys: [.isRegularFileKey])
                guard resourceValues?.isRegularFile == true else {
                    continue
                }

                let fileExtension = fileURL.pathExtension.lowercased()
                if supportedAudioExtensions.contains(fileExtension) {
                    let destination = audioDirectoryURL().appendingPathComponent(sanitizedFilename(fileURL.lastPathComponent))
                    _ = try copyFileContents(from: fileURL, to: destination)
                    importedAudioBaseNames.append(destination.deletingPathExtension().lastPathComponent)
                } else if fileExtension == "ttml" {
                    let destination = lyricsDirectoryURL().appendingPathComponent(sanitizedFilename(fileURL.lastPathComponent))
                    _ = try copyFileContents(from: fileURL, to: destination)
                }
            }

            return importedAudioBaseNames.sorted {
                $0.localizedCaseInsensitiveCompare($1) == .orderedAscending
            }
        }
    }

    private func importSiblingLyricsIfAvailable(for externalAudioURL: URL, baseName: String) throws {
        guard let siblingLyricsURL = try findSiblingLyricsURL(for: externalAudioURL) else {
            return
        }

        let destination = lyricsDirectoryURL().appendingPathComponent("\(baseName).ttml")
        _ = try importFile(from: siblingLyricsURL, to: destination)
    }

    private func findSiblingLyricsURL(for externalAudioURL: URL) throws -> URL? {
        let exactMatch = externalAudioURL.deletingPathExtension().appendingPathExtension("ttml")
        if try scopedFileExists(at: exactMatch) {
            return exactMatch
        }

        let parentDirectoryURL = externalAudioURL.deletingLastPathComponent()
        let audioBaseName = externalAudioURL.deletingPathExtension().lastPathComponent

        return try withSecurityScopedAccess(to: parentDirectoryURL) {
            let siblings = (try? FileManager.default.contentsOfDirectory(at: parentDirectoryURL, includingPropertiesForKeys: nil)) ?? []
            return siblings.first {
                $0.pathExtension.lowercased() == "ttml" &&
                compareBaseName($0.deletingPathExtension().lastPathComponent, audioBaseName)
            }
        }
    }

    private func importFile(from externalURL: URL, to destination: URL) throws -> URL {
        ensureLibraryDirectories()

        return try withSecurityScopedAccess(to: externalURL) {
            try copyFileContents(from: externalURL, to: destination)
        }
    }

    private func copyFileContents(from sourceURL: URL, to destination: URL) throws -> URL {
        let fileManager = FileManager.default
        if fileManager.fileExists(atPath: destination.path) {
            try fileManager.removeItem(at: destination)
        }
        try fileManager.copyItem(at: sourceURL, to: destination)
        return destination
    }

    private func scopedFileExists(at url: URL) throws -> Bool {
        try withSecurityScopedAccess(to: url) {
            FileManager.default.fileExists(atPath: url.path)
        }
    }

    private func withSecurityScopedAccess<T>(to url: URL, operation: () throws -> T) throws -> T {
        let needsScope = url.startAccessingSecurityScopedResource()
        defer {
            if needsScope {
                url.stopAccessingSecurityScopedResource()
            }
        }
        return try operation()
    }

    private func sanitizedFilename(_ filename: String) -> String {
        let invalidCharacters = CharacterSet(charactersIn: "/:\\?%*|\"<>")
        let cleaned = filename.components(separatedBy: invalidCharacters).joined(separator: "_")
        return cleaned.isEmpty ? UUID().uuidString : cleaned
    }

    private func compareBaseName(_ lhs: String, _ rhs: String) -> Bool {
        lhs.caseInsensitiveCompare(rhs) == .orderedSame
    }

    private func isSupportedAudioURL(_ url: URL) -> Bool {
        supportedAudioExtensions.contains(url.pathExtension.lowercased())
    }
}

private extension CMTime {
    var isNumeric: Bool {
        flags.contains(.valid) && !seconds.isNaN && !seconds.isInfinite
    }
}
