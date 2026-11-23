package agnesse.results;

import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.results.SlimExceptionResult;
import agnesse.testsystems.slim.results.SlimTestResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockJsonTest {

  @Test
  public void aTableKeepsOneBasedCellsAndTheCheckpointSixStatuses() {
    MarkdownTable table = MarkdownTable.parse("| script | echo |\n| --- | --- |\n| check | ok |\n| note | skip |\n");
    table.locate("Suite.Page", "script");
    table.updateContent(1, 0, (SlimTestResult) SlimTestResult.pass("ok", "ok"));
    table.updateContent(1, 1, SlimTestResult.pass("$total<-[3]"));
    table.updateContent(0, 2, SlimTestResult.ignore());
    table.updateContent(1, 2, new SlimExceptionResult("id",
      "message:<<java.lang.RuntimeException: boom\n\tat Demo.main(Demo.java:1)>>"));

    JSONObject block = BlockJson.from(table);
    assertEquals("script", block.getString("name"));
    JSONObject pass = cell(block, 0, 1);
    assertEquals(1, pass.getInt("row"));
    assertEquals(2, pass.getInt("column"));
    assertEquals("pass", pass.getString("status"));
    assertEquals("ok", pass.getString("actual"));
    assertEquals("ok", pass.getString("expected"));

    JSONObject symbol = cell(block, 1, 1);
    assertEquals("symbol", symbol.getString("status"));
    assertEquals("total", symbol.getString("symbol"));
    assertEquals("3", symbol.getString("value"));

    assertEquals("ignore", cell(block, 2, 0).getString("status"));
    assertEquals("error", block.getString("status"));
    assertEquals(1, block.getJSONObject("counts").getInt("right"));
    assertEquals(1, block.getJSONObject("counts").getInt("exceptions"));

    JSONObject error = cell(block, 2, 1).getJSONObject("error");
    assertEquals("EXCEPTION", error.getString("kind"));
    assertEquals("java.lang.RuntimeException", error.getString("type"));
    assertTrue(error.getString("stackTrace").contains("Demo.main"));
    assertEquals(3, error.getInt("row"));
    assertEquals(2, error.getInt("cell"));
    assertEquals("Suite.Page", error.getString("page"));
  }

  @Test
  public void aPassingCallThatDoesNotCompareIsNotAnAssertion() {
    MarkdownTable table = MarkdownTable.parse("| script | EchoFixture |\n");
    table.updateContent(1, 0, SlimTestResult.ok("EchoFixture"));
    JSONObject block = BlockJson.from(table);
    assertEquals(JSONObject.NULL, cell(block, 0, 1).get("status"));
    assertEquals(0, block.getJSONObject("counts").getInt("right"));
    assertEquals(JSONObject.NULL, block.get("status"));
  }

  @Test
  public void aBrokenTableWithNoAssertionsIsATableFailure() {
    MarkdownTable table = MarkdownTable.parse("| script | NoSuchFixture |\n");
    table.updateContent(1, 0, new SlimExceptionResult("id", "NO_CLASS NoSuchFixture"));
    JSONObject block = BlockJson.from(table);
    assertEquals(JSONObject.NULL, cell(block, 0, 1).get("status"));
    assertEquals("table-failure", block.getString("status"));
    assertEquals(0, block.getJSONObject("counts").getInt("exceptions"));
  }

  @Test
  public void oneOfEachCountedStatusKeepsItsOutcome() {
    assertEquals("fail", statusOf(SlimTestResult.fail("no", "yes")));
    assertEquals("error", statusOf(SlimTestResult.error("boom", "x")));
    assertEquals("pass", statusOf((SlimTestResult) SlimTestResult.pass("ok", "ok")));
    assertEquals("ignore", statusOf(SlimTestResult.ignore("skip")));
    assertEquals("table-failure", statusOf(SlimTestResult.error("bad table")));
    assertEquals("script", BlockJson.from(MarkdownTable.parse("| script |\n")).getString("name"));
  }

  private static String statusOf(SlimTestResult result) {
    MarkdownTable table = MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, result);
    return BlockJson.from(table).getString("status");
  }

  @Test
  public void aPropertyTableKeepsWordsThatAreNotResults() {
    MarkdownTable table = MarkdownTable.parse("| property |\n| price | true |\n");
    table.updateContent(1, 1, SlimTestResult.fail("false", "true"));
    table.addColumnToRow(0, "<div class=\"rendered-test\">1.00</div>");

    JSONObject block = BlockJson.from(table);

    assertEquals("price", cell(block, 1, 0).getString("value"));
    assertTrue(cell(block, 1, 1).isNull("value"));
    assertEquals("false", cell(block, 1, 1).getString("actual"));
    assertEquals("property", cell(block, 0, 0).getString("value"));
    assertTrue(block.toString(), block.toString().contains("rendered-test"));
  }

  @Test
  public void aScriptCellKeepsTheWordsOnThePage() {
    MarkdownTable table = MarkdownTable.parse("| script |\n| write | hello |\n");
    table.addColumnToRow(1, "<div class=\"rendered-test\">1.00</div>");

    JSONObject block = BlockJson.from(table);

    assertEquals("write", cell(block, 1, 0).getString("value"));
    assertEquals("hello", cell(block, 1, 1).getString("value"));
    assertTrue(block.toString(), block.toString().contains("rendered-test"));
  }

  @Test
  public void anEmptyCellIsNotCopiedAndASecretSymbolIsMasked() {
    MarkdownTable empty = MarkdownTable.parse("| script | | kept |\n");
    assertTrue(cell(BlockJson.from(empty), 0, 1).isNull("value"));
    assertEquals("kept", cell(BlockJson.from(empty), 0, 2).getString("value"));

    MarkdownTable secret = MarkdownTable.parse("| script | token |\n");
    secret.updateContent(1, 0, SlimTestResult.pass("$SECRET_token<-[hidden]"));
    JSONObject symbol = cell(BlockJson.from(secret), 0, 1);
    assertEquals("SECRET_token", symbol.getString("symbol"));
    assertEquals("*****", symbol.getString("value"));
  }

  @Test
  public void aSymbolThatOnlyStartsWithSecretKeepsItsValue() {
    MarkdownTable table = MarkdownTable.parse("| script | SECRETx |\n");
    table.updateContent(1, 0, SlimTestResult.pass("$SECRETx<-[shown]"));

    JSONObject symbol = cell(BlockJson.from(table), 0, 1);

    assertEquals("SECRETx", symbol.getString("symbol"));
    assertEquals("shown", symbol.getString("value"));
  }

  @Test
  public void aScenarioCallKeepsItsBodyAndColorsItFromNestedAssertions() {
    MarkdownTable parent = MarkdownTable.parse("| script |\n| pass |\n| fail |\n| note |\n| boom |\n");
    parent.appendChildTable(1, checked("ok", "ok", true));
    parent.appendChildTable(2, checked("no", "yes", false));
    parent.appendChildTable(3, MarkdownTable.parse("| script |\n| show |\n"));
    MarkdownTable ignored = MarkdownTable.parse("| script |\n| skip |\n");
    ignored.updateContent(0, 1, SlimTestResult.ignore("skip"));
    MarkdownTable holder = MarkdownTable.parse("| script |\n| again |\n");
    holder.appendChildTable(1, ignored);
    parent.appendChildTable(1, holder);
    parent.appendChildTable(4, errored());

    JSONObject block = BlockJson.from(parent);

    assertFalse(block.getJSONArray("rows").getJSONObject(0).has("scenario"));
    assertEquals("pass", scenario(block, 1).getString("status"));
    assertEquals("fail", scenario(block, 2).getString("status"));
    assertTrue(scenario(block, 3).isNull("status"));
    assertEquals("error", scenario(block, 4).getString("status"));
    assertEquals("ok", scenario(block, 1).getJSONArray("blocks").getJSONObject(0)
      .getJSONArray("rows").getJSONObject(1).getJSONArray("cells").getJSONObject(1).getString("actual"));
    assertEquals(0, block.getJSONObject("counts").getInt("right"));
    assertEquals(1, PageCounts.counts(new org.json.JSONArray().put(block)).getInt("right"));
    assertEquals(1, PageCounts.counts(new org.json.JSONArray().put(block)).getInt("wrong"));
    assertEquals(1, PageCounts.counts(new org.json.JSONArray().put(block)).getInt("ignored"));
    assertEquals(1, PageCounts.counts(new org.json.JSONArray().put(block)).getInt("exceptions"));
  }

  @Test
  public void aMissingMethodIsAnExceptionEvenWhenAnotherCheckPasses() {
    MarkdownTable table = MarkdownTable.parse("| script |\n| check | echo | ok | ok |\n| check | missing; | x | true |\n");
    table.updateContent(3, 1, (SlimTestResult) SlimTestResult.pass("ok", "ok"));
    table.updateContent(3, 2, new SlimExceptionResult("id",
      "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS missing[1] agnesse.fixtures.EchoFixture>>"));

    JSONObject block = BlockJson.from(table);
    JSONObject thrown = cell(block, 2, 3);
    assertEquals("error", thrown.getString("status"));
    assertEquals("MISSING_METHOD", thrown.getJSONObject("error").getString("kind"));
    assertEquals(1, block.getJSONObject("counts").getInt("right"));
    assertEquals(1, block.getJSONObject("counts").getInt("exceptions"));
    assertEquals(0, block.getJSONObject("counts").getInt("wrong"));
    JSONObject counts = PageCounts.counts(new JSONArray().put(block));
    assertEquals("error", PageCounts.status("MissingMethod", counts));
    assertEquals(1, counts.getInt("exceptions"));
  }

  @Test
  public void aNestedTableFailureStillCountsOnThePage() {
    MarkdownTable parent = MarkdownTable.parse("| script |\n| call |\n");
    MarkdownTable broken = MarkdownTable.parse("| script | Missing |\n");
    broken.updateContent(1, 0, new SlimExceptionResult("id", "NO_CLASS Missing"));
    parent.appendChildTable(1, broken);
    JSONObject quiet = new JSONObject()
      .put("counts", new JSONObject().put("right", 0).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("status", "pass")
      .put("rows", new JSONArray().put(new JSONObject()
        .put("cells", new JSONArray())
        .put("scenario", new JSONObject().put("status", JSONObject.NULL))));
    JSONObject bare = new JSONObject()
      .put("counts", new JSONObject().put("right", 0).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("status", "pass");
    JSONArray blocks = new JSONArray().put(BlockJson.from(parent)).put(quiet).put(bare);
    JSONObject counts = PageCounts.counts(blocks);
    assertEquals(1, counts.getInt("tableFailures"));
    assertEquals(0, counts.getInt("exceptions"));
    assertEquals(0, counts.getInt("right"));
  }

  @Test
  public void anEmptyTableHasNoName() {
    JSONObject block = BlockJson.from(MarkdownTable.parse(""));
    assertEquals("", block.getString("name"));
    assertEquals(0, block.getJSONArray("rows").length());
  }

  private static JSONObject cell(JSONObject block, int row, int column) {
    return block.getJSONArray("rows").getJSONObject(row).getJSONArray("cells").getJSONObject(column);
  }

  private static JSONObject scenario(JSONObject block, int row) {
    return block.getJSONArray("rows").getJSONObject(row).getJSONObject("scenario");
  }

  private static MarkdownTable checked(String actual, String expected, boolean pass) {
    MarkdownTable table = MarkdownTable.parse("| script |\n| check | value |\n");
    table.updateContent(1, 1, pass
      ? (SlimTestResult) SlimTestResult.pass(actual, expected)
      : SlimTestResult.fail(actual, expected));
    return table;
  }

  private static MarkdownTable errored() {
    MarkdownTable table = MarkdownTable.parse("| script |\n| check | value |\n");
    table.updateContent(1, 1, SlimTestResult.error("boom", "x"));
    return table;
  }
}
