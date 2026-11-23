package agnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks a results document against the checked-in schema's required shape. */
public class ResultsSchemaTest {

  @Test
  public void aReducedDocumentMatchesTheCheckedInSchema() throws Exception {
    JSONObject schema;
    try (InputStream in = RunDocument.class.getResourceAsStream("results.schema.json")) {
      assertNotNull(in);
      schema = new JSONObject(new JSONTokener(in));
    }
    JSONObject document = RunDocument.reduce(java.util.List.of(
      new JSONObject().put("event", "table-done").put("path", "Page").put("block",
        new JSONObject().put("name", "script").put("status", "pass")
          .put("counts", new JSONObject().put("right", 1).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
          .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(
            new JSONObject().put("row", 1).put("column", 1).put("status", "pass")))))),
      new JSONObject().put("event", "page-done").put("path", "Page")
        .put("counts", RunDocument.countsJson(new agnesse.testsystems.TestSummary(1, 0, 0, 0)))
        .put("durationMillis", 3).put("status", "pass")
    ), "id", "Suite", "2026-11-14T00:00:00Z", 3);

    assertEquals(schema.getJSONObject("properties").getJSONObject("schemaVersion").getInt("const"),
      document.getInt("schemaVersion"));
    require(schema.getJSONObject("$defs").getJSONObject("run"), document.getJSONObject("run"));
    SchemaCheck.pagesThatAssertHaveBlocks(document.getJSONObject("run"));
  }

  @Test(expected = IllegalStateException.class)
  public void aPageWithAssertionsAndNoBlocksIsRejected() throws Exception {
    SchemaCheck.main(new String[]{"test/agnesse/results/assertions-without-blocks.json"});
  }

  @Test
  public void matchingCellsOfEveryAssertionStatusAreAccepted() {
    JSONObject cell = new JSONObject().put("row", 1).put("column", 1);
    JSONObject pass = new JSONObject(cell.toString()).put("status", "pass");
    JSONObject fail = new JSONObject(cell.toString()).put("status", "fail");
    JSONObject ignore = new JSONObject(cell.toString()).put("status", "ignore");
    JSONObject error = new JSONObject(cell.toString()).put("status", "error");
    JSONObject compared = new JSONObject().put("name", "script").put("status", "fail")
      .put("counts", new JSONObject().put("right", 1).put("wrong", 1).put("ignored", 1).put("exceptions", 1))
      .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(pass).put(fail).put(ignore).put(error))));
    JSONObject broken = new JSONObject().put("name", "Missing").put("status", "table-failure")
      .put("counts", new JSONObject().put("right", 0).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray());
    JSONObject page = new JSONObject().put("path", "Demo").put("status", "fail").put("durationMillis", 1)
      .put("counts", PageCounts.counts(1, 1, 1, 1, 1))
      .put("blocks", new JSONArray().put(compared).put(broken));
    JSONObject run = new JSONObject().put("pages", new JSONArray().put(page)).put("counts", PageCounts.counts(1, 1, 1, 1, 1));
    SchemaCheck.countsMatchCells(run);
  }

  @Test
  public void nestedScenarioCellsCountTowardThePage() {
    JSONObject cell = new JSONObject().put("row", 1).put("column", 1).put("status", "fail");
    JSONObject child = new JSONObject().put("name", "script").put("status", "fail")
      .put("counts", new JSONObject().put("right", 0).put("wrong", 1).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(cell))));
    JSONObject scenario = new JSONObject().put("status", "fail").put("blocks", new JSONArray().put(child));
    JSONObject row = new JSONObject().put("cells", new JSONArray().put(
      new JSONObject().put("row", 1).put("column", 1).put("status", "pass"))).put("scenario", scenario);
    JSONObject missing = new JSONObject().put("status", JSONObject.NULL);
    JSONObject plain = new JSONObject().put("cells", new JSONArray()).put("scenario", missing);
    JSONObject block = new JSONObject().put("name", "script").put("status", "pass")
      .put("counts", new JSONObject().put("right", 1).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray().put(row).put(plain));
    JSONObject counts = PageCounts.counts(new JSONArray().put(block));
    JSONObject page = new JSONObject().put("path", "Demo").put("status", "fail").put("blocks", new JSONArray().put(block))
      .put("counts", counts);
    SchemaCheck.countsMatchCells(new JSONObject().put("pages", new JSONArray().put(page)).put("counts", counts));
    assertEquals(1, counts.getInt("right"));
    assertEquals(1, counts.getInt("wrong"));
  }

  @Test
  public void aPassingPageCannotHideATableFailure() {
    JSONObject broken = new JSONObject().put("name", "Missing").put("status", "table-failure")
      .put("counts", new JSONObject().put("right", 0).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray());
    JSONObject counts = PageCounts.counts(new JSONArray().put(broken));
    JSONObject page = new JSONObject().put("path", "Demo").put("status", "pass")
      .put("blocks", new JSONArray().put(broken)).put("counts", counts);
    try {
      SchemaCheck.countsMatchCells(new JSONObject().put("pages", new JSONArray().put(page)).put("counts", counts));
      throw new AssertionError("a table failure on a passing page");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("table failure"));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void cellTotalsThatDoNotMatchPageCountsAreRejected() throws Exception {
    SchemaCheck.main(new String[]{"test/agnesse/results/counts-do-not-match-cells.json"});
  }

  @Test
  public void schemaCheckAcceptsAResultsFile() throws Exception {
    SchemaCheck.main(new String[]{"test/agnesse/results/golden-run.json"});
  }

  @Test(expected = IllegalArgumentException.class)
  public void schemaCheckRequiresOneFile() throws Exception {
    SchemaCheck.main(new String[0]);
  }

  @Test(expected = IllegalStateException.class)
  public void schemaCheckRejectsTheWrongVersion() throws Exception {
    Path file = Files.createTempFile("results", ".json");
    Files.writeString(file, "noise\n{\"schemaVersion\":2,\"run\":{}}");
    SchemaCheck.main(new String[]{file.toString()});
  }

  @Test(expected = IllegalStateException.class)
  public void schemaCheckRejectsARunThatOmitsRequiredFields() throws Exception {
    Path file = Files.createTempFile("results", ".json");
    Files.writeString(file, "{\"schemaVersion\":1,\"run\":{}}");
    SchemaCheck.main(new String[]{file.toString()});
  }

  private static void require(JSONObject definition, JSONObject value) {
    JSONArray required = definition.getJSONArray("required");
    for (int i = 0; i < required.length(); i++) {
      String name = required.getString(i);
      assertTrue(name, value.has(name));
    }
  }
}
