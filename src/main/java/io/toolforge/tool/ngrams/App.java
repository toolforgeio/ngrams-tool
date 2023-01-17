package io.toolforge.tool.ngrams;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sigpwned.discourse.core.util.Discourse;
import com.sigpwned.tabular4j.SpreadsheetFactory;
import com.sigpwned.tabular4j.csv.CsvSpreadsheetFormatFactory;
import com.sigpwned.tabular4j.excel.XlsxSpreadsheetFormatFactory;
import com.sigpwned.tabular4j.io.ByteSink;
import com.sigpwned.tabular4j.model.TabularWorksheetReader;
import com.sigpwned.tabular4j.model.TabularWorksheetRowWriter;
import com.sigpwned.tabular4j.model.WorksheetCellDefinition;
import com.sigpwned.uax29.Token;
import com.sigpwned.uax29.UAX29URLEmailTokenizer;
import io.toolforge.toolforge4j.io.OutputSink;

public class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  private static final int MAX_UNIQUE_NGRAM_COUNT = 1000000;

  private static final String CSV = CsvSpreadsheetFormatFactory.DEFAULT_FILE_EXTENSION;

  private static final String XLSX = XlsxSpreadsheetFormatFactory.DEFAULT_FILE_EXTENSION;

  public static void main(String[] args) throws Exception {
    Configuration configuration = Discourse.configuration(Configuration.class, args).validate();
    if (configuration.maxNgramLength < configuration.minNgramLength)
      throw new IllegalArgumentException(
          "MaxNgramLength must be greater than or equal to MinNgramLength");
    main(configuration);
  }

  public static void main(Configuration configuration) throws Exception {
    List<NgramCount> ngrams;
    AtomicLong count = new AtomicLong(0L);
    try (TabularWorksheetReader rows = SpreadsheetFactory.getInstance()
        .readActiveTabularWorksheet(configuration.data::getInputStream)) {
      final int textColumnIndex = rows.findColumnName(configuration.textColumnName)
          .orElseThrow(() -> new IllegalArgumentException(
              "No text column with name " + configuration.textColumnName));
      if (LOGGER.isInfoEnabled())
        LOGGER.info("Reading text from column {} at index {}.", configuration.textColumnName,
            textColumnIndex);
      ngrams = compute(
          rows.stream().parallel().peek(r -> count.incrementAndGet())
              .map(r -> r.getCell(textColumnIndex).getValue(String.class)),
          configuration.minNgramLength.intValue(), configuration.maxNgramLength.intValue())
              .sorted(Comparator.<NgramCount>naturalOrder().reversed()).toList();
    }

    if (LOGGER.isInfoEnabled())
      LOGGER.info("Found {} unique ngrams in {} spreadsheet rows.", ngrams.size(), count.get());

    if (ngrams.size() > MAX_UNIQUE_NGRAM_COUNT) {
      if (LOGGER.isWarnEnabled())
        LOGGER.warn("Truncating output to {} most common unique ngrams...", MAX_UNIQUE_NGRAM_COUNT);
      ngrams = ngrams.subList(0, MAX_UNIQUE_NGRAM_COUNT);
    }

    Map<String, OutputSink> outputFormats =
        Map.of(CSV, configuration.ngramsCsv, XLSX, configuration.ngramsXlsx);
    for (var outputFormat : outputFormats.entrySet()) {
      if (LOGGER.isInfoEnabled())
        LOGGER.info("Outputting {} report...", outputFormat);
      String outputFormatName = outputFormat.getKey();
      ByteSink outputFormatSink = outputFormat.getValue()::getOutputStream;
      try (TabularWorksheetRowWriter w = SpreadsheetFactory.getInstance()
          .writeTabularActiveWorksheet(outputFormatSink, outputFormatName)
          .writeHeaders("ngram", "count")) {
        for (NgramCount ngram : ngrams) {
          w.writeRow(List.of(WorksheetCellDefinition.ofValue(ngram.ngram()),
              WorksheetCellDefinition.ofValue(ngram.count())));
        }
      }
    }

    if (LOGGER.isInfoEnabled())
      LOGGER.info("Done!");
  }

  public static record NgramCount(String ngram, long count) implements Comparable<NgramCount> {
    public static NgramCount of(String ngram, long count) {
      return new NgramCount(ngram, count);
    }

    public NgramCount(String ngram, long count) {
      if (count < 1L)
        throw new IllegalArgumentException("count must be positive");
      this.ngram = requireNonNull(ngram);
      this.count = count;
    }

    private static final Comparator<NgramCount> COMPARATOR =
        Comparator.comparingLong(NgramCount::count).thenComparingInt(c -> c.ngram().hashCode());

    @Override
    public int compareTo(NgramCount that) {
      return COMPARATOR.compare(this, that);
    }
  }

  public static Stream<NgramCount> compute(Stream<String> texts, int minNgramLength,
      int maxNgramLength) {
    return texts.filter(Objects::nonNull).filter(not(String::isBlank))
        .flatMap(s -> ngrams(s, minNgramLength, maxNgramLength))
        .collect(groupingBy(identity(), counting())).entrySet().stream()
        .map(e -> NgramCount.of(e.getKey(), e.getValue()));
  }

  public static Stream<String> ngrams(String text, int minLength, int maxLength) {
    return ngrams(tokens(text), minLength, maxLength);
  }

  public static List<String> tokens(String text) {
    List<String> result = new ArrayList<>();
    try {
      try (UAX29URLEmailTokenizer tokenizer = new UAX29URLEmailTokenizer(text.toLowerCase())) {
        for (Token token = tokenizer.nextToken(); token != null; token =
            tokenizer.nextToken(token)) {
          result.add(token.getText());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to tokenize text", e);
    }
    return result;
  }

  public static Stream<String> ngrams(List<String> tokens, int minLength, int maxLength) {
    return IntStream.range(0, tokens.size()).boxed().flatMap(
        i -> IntStream.rangeClosed(minLength, maxLength).filter(len -> i + len < tokens.size())
            .mapToObj(len -> String.join(" ", tokens.subList(i, i + len))));
  }
}
