package agnesse.testsystems.slim.tables;

import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.slim.MarkdownTableScanner;
import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.SlimTestContextImpl;
import agnesse.testsystems.slim.Table;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DecisionTableUsesTheLastScriptSubclassTest {

  @Test
  public void aDecisionTableUsesTheLastScriptsActorAndCheckKeyword() throws Exception {
    SlimTableFactory factory = new SlimTableFactory();
    factory.addTableType("verify script", VerifyScriptTable.class);
    var root = InMemoryPage.makeRoot("root");
    WikiPageUtil.setPageContents(root, """
      | scenario | divide | expected |
      | --- | --- | --- |
      | verify | quotient | @expected |

      | script | agnesse.slim.test.Division |
      | --- | --- |

      | verify script | agnesse.slim.test.Division |
      | --- | --- |

      | divide |
      | --- | --- |
      | expected |
      | 5.0 |
      """);
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(root));
    MarkdownTableScanner scanner = new MarkdownTableScanner(root.getData().getContent());
    List<SlimAssertion> assertions = new ArrayList<>();
    for (int i = 0; i < scanner.getTableCount(); i++) {
      SlimTable table = factory.makeSlimTable(scanner.getTable(i), "t" + i, context);
      assertions.addAll(table.getAssertions());
    }

    String calls = SlimAssertion.getInstructions(assertions).toString();
    assertTrue(calls, calls.contains("instanceName='verifyScriptTableActor', methodName='quotient'"));
    assertFalse(calls, calls.contains("instanceName='scriptTableActor', methodName='quotient'"));
  }

  /** A script table whose check keyword is {@code verify}, registered only by the test. */
  public static class VerifyScriptTable extends ScriptTable {
    public VerifyScriptTable(Table table, String tableId, SlimTestContext context) {
      super(table, tableId, context);
    }

    @Override
    protected String getTableType() {
      return "verifyScriptTable";
    }

    @Override
    protected String getCheckKeyword() {
      return "verify";
    }
  }
}
