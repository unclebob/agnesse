package agnesse.markdown;

import agnesse.testsystems.slim.tables.Disgracer;
import agnesse.wiki.PathParser;
import agnesse.wiki.TestAdjunct;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageUtil;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scenario tables a viewed page can call, in the order a run would read
 * them before TearDown: libraries outer first, then SetUp, then the page.
 * A later definition replaces an earlier one. {@code scenarios} and
 * {@code include} pull in another page. The body is the definition as
 * written; call arguments are not substituted, and a {@code _} name is
 * not pattern-matched.
 */
final class ScenarioView {
  private static final Parser PARSER = Parser.builder().extensions(List.of(TablesExtension.create())).build();
  private static final Set<String> KEYWORDS = Set.of(
    "start", "check", "check not", "ensure", "reject", "show", "note");

  private final Map<String, Scenario> scenarios = new HashMap<>();
  private final Set<String> open = new HashSet<>();

  static ScenarioView empty() {
    return new ScenarioView();
  }

  static ScenarioView collect(MarkdownPage page) {
    ScenarioView view = new ScenarioView();
    if (page == null || !WikiPageUtil.rendersSlimTables(page)) {
      return view;
    }
    Set<String> seen = new HashSet<>();
    view.libraries(page, seen);
    view.setup(page, seen);
    view.add(page, page, seen);
    return view;
  }

  Scenario match(String type, String tableName, int row, List<String> cells) {
    if (cells.isEmpty()) {
      return null;
    }
    if ("script".equals(type)) {
      return scriptCall(row, cells);
    }
    return decisionCall(type, tableName, row);
  }

  boolean enter(String name) {
    return open.add(name);
  }

  void leave(String name) {
    open.remove(name);
  }

  /** Reads one page. A non-markdown page contributes nothing. */
  void take(WikiPage page, MarkdownPage context) {
    add(page, context, new HashSet<>());
  }

  private void libraries(MarkdownPage page, Set<String> seen) {
    if (!includeLibraries(page)) {
      return;
    }
    List<WikiPage> libraries = new ArrayList<>();
    page.getPageCrawler().traverseUncles(TestAdjunct.SCENARIO_LIBRARY, libraries::addFirst);
    for (WikiPage library : libraries) {
      add(library, page, seen);
    }
  }

  private void setup(MarkdownPage page, Set<String> seen) {
    if (suiteBoundary(page)) {
      return;
    }
    WikiPage setup = page.getPageCrawler().getClosestInheritedPage(TestAdjunct.SET_UP);
    if (setup != null) {
      add(setup, page, seen);
    }
  }

  private void add(WikiPage page, MarkdownPage context, Set<String> seen) {
    if (!(page instanceof MarkdownPage markdown)) {
      return;
    }
    String id = PathParser.render(markdown.getFullPath());
    if (!seen.add(id)) {
      return;
    }
    try {
      read(markdown.expandedForSlim(context), markdown, context, seen);
    } finally {
      seen.remove(id);
    }
  }

  private void read(String content, MarkdownPage owner, MarkdownPage context, Set<String> seen) {
    StringBuilder tables = new StringBuilder();
    StringBuilder fenceBody = new StringBuilder();
    MarkdownFences fence = new MarkdownFences();
    String info = "";
    for (String line : content.split("\n")) {
      boolean wasInside = fence.inside();
      if (fence.boundary(line)) {
        onFence(wasInside, tables, fenceBody, info, owner, context, seen);
        if (!wasInside) {
          info = line.trim().replaceFirst("^`+", "").trim();
        }
        continue;
      }
      append(wasInside, line, tables, fenceBody);
    }
    recordTables(tables.toString());
  }

  private void onFence(boolean wasInside, StringBuilder tables, StringBuilder fenceBody, String info,
                       MarkdownPage owner, MarkdownPage context, Set<String> seen) {
    if (wasInside) {
      follow(owner, info, fenceBody.toString(), context, seen);
      fenceBody.setLength(0);
      return;
    }
    recordTables(tables.toString());
    tables.setLength(0);
  }

  private static void append(boolean inside, String line, StringBuilder tables, StringBuilder fenceBody) {
    if (inside) {
      fenceBody.append(line).append('\n');
      return;
    }
    tables.append(line).append('\n');
  }

  private void follow(MarkdownPage owner, String info, String body, MarkdownPage context, Set<String> seen) {
    if (!info.startsWith("agnesse")) {
      return;
    }
    String command = command(body);
    if (!"scenarios".equals(command) && !"include".equals(command)) {
      return;
    }
    String target = target(body);
    if (target.isEmpty()) {
      return;
    }
    WikiPagePath path = PathParser.parse(target);
    if (path == null) {
      return;
    }
    add(owner.getPageCrawler().getSiblingPage(path), context, seen);
  }

  private static String command(String body) {
    String first = firstLine(body);
    if (first.isEmpty()) {
      return "";
    }
    return first.split(" ", 2)[0].toLowerCase(Locale.ROOT);
  }

  private static String target(String body) {
    AgNesseBlock block = AgNesseBlock.parse(body);
    String argument = block.argument().trim();
    if (argument.isEmpty()) {
      return "";
    }
    return argument.split("\\s+")[0];
  }

  private static String firstLine(String body) {
    int newline = body.indexOf('\n');
    String first = newline == -1 ? body : body.substring(0, newline);
    return first.trim();
  }

  private void recordTables(String markdown) {
    if (markdown.isBlank()) {
      return;
    }
    recordBlocks(PARSER.parse(TableSeparators.supply(markdown)));
  }

  private void recordBlocks(Node node) {
    if (node instanceof TableBlock block) {
      record(SlimMarkdownTable.from(block));
      return;
    }
    Node child = node.getFirstChild();
    while (child != null) {
      recordBlocks(child);
      child = child.getNext();
    }
  }

  private void record(SlimMarkdownTable table) {
    if (!"scenario".equals(table.type())) {
      return;
    }
    String called = name(table.row(1), 1);
    if (called.isEmpty()) {
      return;
    }
    scenarios.put(called, new Scenario(called, body(table)));
  }

  private static List<List<String>> body(SlimMarkdownTable table) {
    List<List<String>> rows = new ArrayList<>();
    for (int row = 2; row <= table.rowCount(); row++) {
      rows.add(List.copyOf(table.row(row)));
    }
    return List.copyOf(rows);
  }

  private Scenario scriptCall(int row, List<String> cells) {
    if (row <= 0) {
      return null;
    }
    if (keyword(cells.get(0))) {
      return null;
    }
    return scenarios.get(name(cells, 0));
  }

  private Scenario decisionCall(String type, String tableName, int row) {
    if (!"decision".equals(type)) {
      return null;
    }
    if (row <= 1) {
      return null;
    }
    return scenarios.get(Disgracer.disgraceClassName(tableName.trim()));
  }

  private static String name(List<String> cells, int start) {
    StringBuilder joined = new StringBuilder();
    for (int column = start; column < cells.size(); column += 2) {
      String cell = cells.get(column) == null ? "" : cells.get(column).trim();
      if (joined.length() > 0) {
        joined.append(' ');
      }
      joined.append(cell);
      if (cell.endsWith(";")) {
        break;
      }
    }
    return Disgracer.disgraceClassName(joined.toString().replace(";", ""));
  }

  private static boolean keyword(String first) {
    String cell = first == null ? "" : first.trim();
    if (cell.startsWith("#")) {
      return true;
    }
    if (cell.startsWith("*")) {
      return true;
    }
    return KEYWORDS.contains(cell.toLowerCase(Locale.ROOT));
  }

  private static boolean includeLibraries(MarkdownPage page) {
    String value = page.getVariable("INCLUDE_SCENARIO_LIBRARIES");
    if (value == null) {
      return true;
    }
    return !"false".equalsIgnoreCase(value);
  }

  private static boolean suiteBoundary(WikiPage page) {
    if (TestAdjunct.SUITE_SET_UP.equals(page.getName())) {
      return true;
    }
    return TestAdjunct.SUITE_TEAR_DOWN.equals(page.getName());
  }

  record Scenario(String name, List<List<String>> body) {
  }
}
