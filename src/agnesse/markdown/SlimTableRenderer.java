package agnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SlimTableRenderer implements NodeRenderer {
  private static final String TOGGLE =
    "<details class=\"scenario-call\"><summary><button type=\"button\" class=\"scenario-toggle\" aria-expanded=\"false\">"
      + "<span class=\"when-closed\">▸</span><span class=\"when-open\">▾</span></button>";

  private final HtmlNodeRendererContext context;
  private final HtmlWriter writer;
  private final boolean testPage;
  private final RenderState state;
  private final ScenarioView scenarios;
  private int rowIndex;

  SlimTableRenderer(HtmlNodeRendererContext context, boolean testPage, RenderState state, ScenarioView scenarios) {
    this.context = context;
    this.writer = context.getWriter();
    this.testPage = testPage;
    this.state = state;
    this.scenarios = scenarios;
  }

  @Override public Set<Class<? extends Node>> getNodeTypes() { return Set.of(TableBlock.class); }

  @Override
  public void render(Node node) {
    TableBlock block = (TableBlock) node;
    SlimMarkdownTable table = SlimMarkdownTable.from(block);
    String first = table.row(1).get(0);
    writer.line();
    if (!SlimTables.slim(testPage, first)) {
      writePlain(block);
      return;
    }
    if (table.error() != null) {
      writer.raw("<table class=\"agnesse-table error\">\n<tr>\n<td><span class=\"error\">");
      writer.text(table.error());
      writer.raw("</span></td>\n</tr>\n</table>\n");
      return;
    }
    rowIndex = 0;
    writeRows(block, table);
  }

  private void writePlain(TableBlock block) {
    writer.raw("<table>\n");
    Node section = block.getFirstChild();
    while (section != null) {
      if (section instanceof TableHead || section instanceof TableBody) {
        writePlainSection(section, section instanceof TableHead ? "th" : "td");
      }
      section = section.getNext();
    }
    writer.raw("</table>\n");
  }

  private void writePlainSection(Node section, String cellTag) {
    Node row = section.getFirstChild();
    while (row != null) {
      if (row instanceof TableRow) {
        writer.raw("<tr>");
        Node cell = row.getFirstChild();
        while (cell != null) {
          if (cell instanceof TableCell) {
            writer.raw("<" + cellTag + ">");
            renderChildren(cell);
            writer.raw("</" + cellTag + ">");
          }
          cell = cell.getNext();
        }
        writer.raw("</tr>\n");
      }
      row = row.getNext();
    }
  }

  private void writeRows(TableBlock block, SlimMarkdownTable table) {
    writer.raw("<table class=\"agnesse-table " + table.type() + "\">\n");
    Node section = block.getFirstChild();
    while (section != null) {
      if (section instanceof TableHead || section instanceof TableBody) {
        writeSection(section, table);
      }
      section = section.getNext();
    }
    writer.raw("</table>\n");
  }

  private void writeSection(Node section, SlimMarkdownTable table) {
    Node row = section.getFirstChild();
    while (row != null) {
      if (row instanceof TableRow) {
        writeRow((TableRow) row, table, rowIndex);
        rowIndex++;
      }
      row = row.getNext();
    }
  }

  private void writeRow(TableRow row, SlimMarkdownTable table, int index) {
    List<TableCell> cells = withoutTrailingEmpty(cells(row));
    ScenarioView.Scenario scenario = scenarios.match(table.type(), table.name(), index, texts(cells));
    if (scenario == null) {
      writeCellsRow(cells);
      return;
    }
    writeWrapped(cells.size(), scenario, () -> writeCells(cells));
  }

  private void writeCellsRow(List<TableCell> cells) {
    writer.raw("<tr>");
    writeCells(cells);
    writer.raw("</tr>\n");
  }

  private void writeCells(List<TableCell> cells) {
    for (TableCell cell : cells) {
      writer.raw("<td data-slim=\"" + attribute(PlainText.of(cell).trim()) + "\">");
      state.literalCells = true;
      renderChildren(cell);
      state.literalCells = false;
      writer.raw("</td>");
    }
  }

  private void writeWrapped(int columns, ScenarioView.Scenario scenario, Runnable line) {
    writer.raw("<tr><td colspan=\"" + columns + "\">");
    writer.raw(TOGGLE);
    writer.raw("<table class=\"agnesse-table scenario-line\"><tr>");
    line.run();
    writer.raw("</tr></table></summary><div class=\"scenario-body\">");
    writeBody(scenario);
    writer.raw("</div></details></td></tr>\n");
  }

  private void writeBody(ScenarioView.Scenario scenario) {
    writer.raw("<table class=\"agnesse-table script\">\n");
    if (!scenarios.enter(scenario.name())) {
      writeFlat(scenario);
      writer.raw("</table>\n");
      return;
    }
    try {
      for (List<String> row : scenario.body()) {
        writeBodyRow(row);
      }
    } finally {
      scenarios.leave(scenario.name());
    }
    writer.raw("</table>\n");
  }

  private void writeFlat(ScenarioView.Scenario scenario) {
    for (List<String> row : scenario.body()) {
      writeTextRow(trimmed(row));
    }
  }

  private void writeBodyRow(List<String> row) {
    List<String> cells = trimmed(row);
    ScenarioView.Scenario nested = scenarios.match("script", "", 1, cells);
    if (nested == null) {
      writeTextRow(cells);
      return;
    }
    writeWrapped(cells.size(), nested, () -> writeTextCells(cells));
  }

  private void writeTextRow(List<String> cells) {
    writer.raw("<tr>");
    writeTextCells(cells);
    writer.raw("</tr>\n");
  }

  private void writeTextCells(List<String> cells) {
    for (String cell : cells) {
      writer.raw("<td>");
      writer.text(cell);
      writer.raw("</td>");
    }
  }

  private static List<String> texts(List<TableCell> cells) {
    List<String> texts = new ArrayList<>();
    for (TableCell cell : cells) {
      texts.add(PlainText.of(cell));
    }
    return texts;
  }

  private static List<String> trimmed(List<String> row) {
    int last = row.size() - 1;
    while (last > 0 && row.get(last).trim().isEmpty()) {
      last--;
    }
    List<String> kept = new ArrayList<>();
    for (int i = 0; i <= last; i++) {
      kept.add(row.get(i));
    }
    return kept;
  }

  private void renderChildren(Node node) {
    Node child = node.getFirstChild();
    while (child != null) {
      context.render(child);
      child = child.getNext();
    }
  }

  private static List<TableCell> cells(TableRow row) {
    List<TableCell> cells = new ArrayList<>();
    Node cell = row.getFirstChild();
    while (cell != null) {
      if (cell instanceof TableCell) {
        cells.add((TableCell) cell);
      }
      cell = cell.getNext();
    }
    return cells;
  }

  private static List<TableCell> withoutTrailingEmpty(List<TableCell> row) {
    int last = row.size() - 1;
    while (last > 0 && PlainText.of(row.get(last)).trim().isEmpty()) {
      last--;
    }
    return new ArrayList<>(row.subList(0, last + 1));
  }

  private static String attribute(String text) {
    return text.replace("&", "&amp;").replace("\"", "&quot;").replace("\n", "&#10;");
  }
}
