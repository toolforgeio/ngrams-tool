# NGRAMS TOOL [![tests](https://github.com/toolforgeio/ngrams-tool/actions/workflows/tests.yml/badge.svg)](https://github.com/toolforgeio/ngrams-tool/actions/workflows/tests.yml)

The NGrams Tool performs a [frequency
analysis](https://en.wikipedia.org/wiki/Frequency_analysis) of the
[ngrams](https://en.wikipedia.org/wiki/N-gram) that make up a [text
corpus](https://en.wikipedia.org/wiki/Text_corpus) contained in one
column of a spreadsheet.

## Example Usage

For the following parameters:

    MinNgramLength: 1
    MaxNgramLength: 3

For the corpus:

    There, peeping among the cloud-wrack above a dark tower high up in
    the mountains, Sam saw a white star twinkle for a while. The
    beauty of it smote his heart, as he looked up out of the forsaken
    land, and hope returned to him. For like a shaft, clear and cold,
    the thought pierced him that in the end the Shadow was only a
    small and passing thing: there was light and high beauty for ever
    beyond its reach.

The first 25 rows of output would be as follows:

|       ngram       | length | count |
| ----------------- | ------ | ----- |
| the               |   1    |   7   |
| a                 |   1    |   5   |
| and               |   1    |   4   |
| for               |   1    |   3   |
| there             |   1    |   2   |
| high              |   1    |   2   |
| was               |   1    |   2   |
| him               |   1    |   2   |
| up                |   1    |   2   |
| of                |   1    |   2   |
| in                |   1    |   2   |
| in the            |   2    |   2   |
| beauty            |   1    |   2   |
| the forsaken land |   3    |   1   |
| the cloud         |   2    |   1   |
| cold the thought  |   3    |   1   |
| him that in       |   3    |   1   |
| returned to him   |   3    |   1   |
| while the beauty  |   3    |   1   |
| sam saw           |   2    |   1   |
| above a dark      |   3    |   1   |
| ever beyond       |   2    |   1   |
| cloud wrack above |   3    |   1   |
| was light and     |   3    |   1   |
| beauty for        |   2    |   1   |

## FAQ

### How is the corpus split into tokens?

The corpus is split into tokens according to the text segmentation
algorithm captured in
[Unicode Standard Annex #29](https://unicode.org/reports/tr29/), i.e.,
UAX29. [The specific implementation](https://github.com/sigpwned/uax29)
used includes some enhancements, namely token types for URLs, emoji,
emails, hashtags, cashtags, and mentions.

This (much) more complex approach is used instead of a (much) more
simple appproach, like splitting on whitespace, because UAX29 performs
useful text segmentation not only on romance languages, but also on
Asian languages.

UAX29 text segmentation rules operate based on whitespace and
punctuation boundaries, so users will likely find its behavior
intuitive for whitespace-delimited languages, like Romance (e.g.,
Spanish, French, Italian) and Germanic (e.g., English, German)
languages.
