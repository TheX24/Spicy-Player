import Foundation

enum TTMLLyricsParser {
    static func parse(data: Data) throws -> ParsedLyrics {
        let delegate = ParserDelegate()
        let parser = XMLParser(data: data)
        parser.shouldProcessNamespaces = true
        parser.delegate = delegate

        guard parser.parse() else {
            throw delegate.error ?? parser.parserError ?? NSError(
                domain: "SpicyPlayeriOS.TTMLParser",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Failed to parse TTML lyrics."]
            )
        }

        return delegate.result()
    }
}

private final class ParserDelegate: NSObject, XMLParserDelegate {
    struct SpanContext {
        let begin: Int?
        let end: Int?
        let isBackground: Bool
    }

    var lines: [LyricLine] = []
    var songwriters: [String] = []
    var error: Error?

    private var defaultAgent: String?
    private var inSongwriter = false
    private var currentParagraph: ParagraphState?

    func result() -> ParsedLyrics {
        var allLines = lines

        if !songwriters.isEmpty {
            let lastLineEnd = allLines.map(\.endMs).max() ?? 0
            let text = "Written by \(songwriters.joined(separator: ", "))"
            let tokens = text.split(separator: " ").map(String.init)
            let words = tokens.map { token in
                LyricWord(
                    text: token,
                    startMs: lastLineEnd,
                    endMs: lastLineEnd + 1000,
                    isPartOfWord: false,
                    isLetterGroup: false,
                    letters: []
                )
            }

            allLines.append(
                LyricLine(
                    words: words,
                    startMs: lastLineEnd,
                    agent: nil,
                    isBackground: false,
                    oppositeAligned: false,
                    isSongwriter: true,
                    isInterlude: false,
                    interludeEndMs: -1
                )
            )
        }

        let mainLines = allLines
            .filter { !$0.isBackground && !$0.isSongwriter }
            .sorted { $0.startMs < $1.startMs }

        var interludes: [LyricLine] = []
        if let first = mainLines.first, first.startMs >= 3000 {
            interludes.append(
                LyricLine(
                    words: [],
                    startMs: 0,
                    agent: nil,
                    isBackground: false,
                    oppositeAligned: false,
                    isSongwriter: false,
                    isInterlude: true,
                    interludeEndMs: first.startMs
                )
            )
        }

        if mainLines.count >= 2 {
            for index in 0..<(mainLines.count - 1) {
                let gapStart = mainLines[index].endMs
                let gapEnd = mainLines[index + 1].startMs
                if gapEnd - gapStart >= 3000 {
                    interludes.append(
                        LyricLine(
                            words: [],
                            startMs: gapStart,
                            agent: nil,
                            isBackground: false,
                            oppositeAligned: false,
                            isSongwriter: false,
                            isInterlude: true,
                            interludeEndMs: gapEnd
                        )
                    )
                }
            }
        }

        allLines.append(contentsOf: interludes)
        allLines.sort { $0.startMs < $1.startMs }

        return ParsedLyrics(lines: allLines, songwriters: songwriters)
    }

    func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
        error = parseError
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        let name = normalizedName(elementName, qName)

        switch name {
        case "songwriter":
            inSongwriter = true
        case "agent":
            let identifier = attributeDict["xml:id"] ?? attributeDict["id"]
            if identifier == "v1" {
                defaultAgent = identifier
            }
        case "p":
            let agent = attributeDict["agent"] ?? attributeDict["ttm:agent"]
            if defaultAgent == nil, let agent {
                defaultAgent = agent
            }
            currentParagraph = ParagraphState(
                beginMs: parseTimeMs(attributeDict["begin"]) ?? 0,
                endMs: parseTimeMs(attributeDict["end"]) ?? 0,
                agent: agent,
                defaultAgent: defaultAgent
            )
        case "span":
            currentParagraph?.pushSpan(attributes: attributeDict)
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if inSongwriter {
            let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                songwriters.append(trimmed)
            }
            return
        }

        currentParagraph?.consume(text: string)
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        let name = normalizedName(elementName, qName)

        switch name {
        case "songwriter":
            inSongwriter = false
        case "span":
            currentParagraph?.popSpan()
        case "p":
            if let paragraph = currentParagraph {
                lines.append(contentsOf: paragraph.makeLines())
            }
            currentParagraph = nil
        default:
            break
        }
    }

    private func normalizedName(_ elementName: String, _ qName: String?) -> String {
        let raw = qName ?? elementName
        return raw.split(separator: ":").last.map(String.init) ?? raw
    }
}

private struct ParagraphState {
    let beginMs: Int
    let endMs: Int
    let agent: String?
    let defaultAgent: String?

    private(set) var leadWords: [LyricWord] = []
    private(set) var backgroundGroups: [[LyricWord]] = []
    private var stack: [ParserDelegate.SpanContext]
    private var previousEndedMidWord = false
    private var currentBackgroundGroup: [LyricWord]?
    private var inBackgroundSpan = false
    private var backgroundPreviousEndedMidWord = false

    init(beginMs: Int, endMs: Int, agent: String?, defaultAgent: String?) {
        self.beginMs = beginMs
        self.endMs = endMs
        self.agent = agent
        self.defaultAgent = defaultAgent
        self.stack = [ParserDelegate.SpanContext(begin: beginMs, end: endMs, isBackground: false)]
    }

    mutating func pushSpan(attributes: [String: String]) {
        let inherited = stack.last ?? ParserDelegate.SpanContext(begin: beginMs, end: endMs, isBackground: false)
        let begin = parseTimeMs(attributes["begin"]) ?? inherited.begin
        let end = parseTimeMs(attributes["end"]) ?? inherited.end
        let role = attributes["role"] ?? attributes["ttm:role"]
        let isBackground = role == "x-bg" || inherited.isBackground

        if isBackground && currentBackgroundGroup == nil {
            currentBackgroundGroup = []
            backgroundPreviousEndedMidWord = false
        }

        inBackgroundSpan = isBackground
        stack.append(ParserDelegate.SpanContext(begin: begin, end: end, isBackground: isBackground))
    }

    mutating func popSpan() {
        guard !stack.isEmpty else {
            return
        }

        let popped = stack.removeLast()
        let nextIsBackground = stack.last?.isBackground ?? false
        if popped.isBackground && !nextIsBackground {
            if let currentBackgroundGroup, !currentBackgroundGroup.isEmpty {
                backgroundGroups.append(currentBackgroundGroup)
            }
            self.currentBackgroundGroup = nil
            inBackgroundSpan = false
        } else {
            inBackgroundSpan = nextIsBackground
        }
    }

    mutating func consume(text rawText: String) {
        let context = stack.last ?? ParserDelegate.SpanContext(begin: beginMs, end: endMs, isBackground: false)
        let isBackgroundToken = context.isBackground || inBackgroundSpan

        if rawText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if isBackgroundToken {
                backgroundPreviousEndedMidWord = false
            } else {
                previousEndedMidWord = false
            }
            return
        }

        let startsWithSpace = rawText.first?.isWhitespace == true
        let endsWithSpace = rawText.last?.isWhitespace == true
        var trimmed = rawText.trimmingCharacters(in: .whitespacesAndNewlines)

        if isBackgroundToken {
            trimmed = trimmed.replacingOccurrences(of: #"^\("#, with: "", options: .regularExpression)
            trimmed = trimmed.replacingOccurrences(of: #"\)$"#, with: "", options: .regularExpression)
        }

        if trimmed.isEmpty {
            return
        }

        let spaceTokens = trimmed
            .split(whereSeparator: { $0.isWhitespace })
            .map(String.init)
            .filter { !$0.isEmpty }

        var wordsToAdd: [(String, Bool)] = []
        for (tokenIndex, token) in spaceTokens.enumerated() {
            let subTokens = splitKeepingTrailingHyphen(token)
            for (subIndex, subToken) in subTokens.enumerated() {
                let isAttached: Bool
                if tokenIndex == 0 && subIndex == 0 {
                    if isBackgroundToken {
                        isAttached = backgroundPreviousEndedMidWord && !startsWithSpace
                    } else {
                        isAttached = previousEndedMidWord && !startsWithSpace
                    }
                } else {
                    isAttached = subIndex > 0
                }
                wordsToAdd.append((subToken, isAttached))
            }
        }

        let start = context.begin ?? beginMs
        let end = context.end ?? (start + 1000)
        let duration = max(end - start, 0)
        let chunkDuration = wordsToAdd.isEmpty ? duration : duration / wordsToAdd.count

        for (index, entry) in wordsToAdd.enumerated() {
            let wordStart = start + (index * chunkDuration)
            let wordEnd = start + ((index + 1) * chunkDuration)
            let wordDuration = wordEnd - wordStart
            let token = entry.0
            let isLetterGroup = wordDuration >= 1000 && token.count > 1 && token.count <= 12

            let letters: [LyricLetter]
            if isLetterGroup {
                let count = max(token.count, 1)
                let letterDuration = Double(wordDuration) / Double(count)
                letters = token.enumerated().map { offset, character in
                    LyricLetter(
                        char: String(character),
                        startMs: wordStart + Int(Double(offset) * letterDuration),
                        endMs: wordStart + Int(Double(offset + 1) * letterDuration)
                    )
                }
            } else {
                letters = []
            }

            let word = LyricWord(
                text: token,
                startMs: wordStart,
                endMs: wordEnd,
                isPartOfWord: entry.1,
                isLetterGroup: isLetterGroup,
                letters: letters
            )

            if isBackgroundToken {
                currentBackgroundGroup?.append(word)
            } else {
                leadWords.append(word)
            }
        }

        if isBackgroundToken {
            backgroundPreviousEndedMidWord = !endsWithSpace
        } else {
            previousEndedMidWord = !endsWithSpace
        }
    }

    func makeLines() -> [LyricLine] {
        let oppositeAligned = agent != nil && defaultAgent != nil && agent != defaultAgent
        var result: [LyricLine] = []

        result.append(
            LyricLine(
                words: leadWords,
                startMs: beginMs,
                agent: agent,
                isBackground: false,
                oppositeAligned: oppositeAligned,
                isSongwriter: false,
                isInterlude: false,
                interludeEndMs: -1
            )
        )

        for group in backgroundGroups {
            let groupStart = group.first?.startMs ?? beginMs
            result.append(
                LyricLine(
                    words: group,
                    startMs: groupStart,
                    agent: agent,
                    isBackground: true,
                    oppositeAligned: oppositeAligned,
                    isSongwriter: false,
                    isInterlude: false,
                    interludeEndMs: -1
                )
            )
        }

        return result
    }

    private func splitKeepingTrailingHyphen(_ token: String) -> [String] {
        [token]
    }
}

private func parseTimeMs(_ time: String?) -> Int? {
    guard let time else {
        return nil
    }

    let parts = time.split(separator: ":").map(String.init)
    if parts.count == 2 {
        let minutes = Int(parts[0]) ?? 0
        let secondsParts = parts[1].split(separator: ".", maxSplits: 1).map(String.init)
        let seconds = Int(secondsParts[0]) ?? 0
        let milliseconds = secondsParts.count > 1 ? paddedMilliseconds(secondsParts[1]) : 0
        return ((minutes * 60) + seconds) * 1000 + milliseconds
    }

    if parts.count == 3 {
        let hours = Int(parts[0]) ?? 0
        let minutes = Int(parts[1]) ?? 0
        let secondsParts = parts[2].split(separator: ".", maxSplits: 1).map(String.init)
        let seconds = Int(secondsParts[0]) ?? 0
        let milliseconds = secondsParts.count > 1 ? paddedMilliseconds(secondsParts[1]) : 0
        return ((hours * 3600) + (minutes * 60) + seconds) * 1000 + milliseconds
    }

    return nil
}

private func paddedMilliseconds(_ raw: String) -> Int {
    let prefix = String(raw.prefix(3))
    let padded = prefix.padding(toLength: 3, withPad: "0", startingAt: 0)
    return Int(padded) ?? 0
}
