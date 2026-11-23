package agnesse.testsystems.slim.results;

import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestResult;
import org.junit.Test;

import java.util.Map;

import static agnesse.slim.SlimServer.EXCEPTION_TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CellRecordTest {

  @Test
  public void aMissingMethodCountsAsAnExceptionAndAMissingFixtureDoesNot() {
    CellRecord missing = CellRecord.error(new SlimExceptionResult("id",
      EXCEPTION_TAG + "message:<<NO_METHOD_IN_CLASS pay[1] agnesse.fixtures.PageDriver>>"));
    assertEquals("error", missing.status());
    assertTrue(missing.assertion());
    assertFalse(missing.trouble());

    CellRecord fixture = CellRecord.error(new SlimExceptionResult("id",
      EXCEPTION_TAG + "message:<<NO_CLASS agnesse.missing.Fixture>>"));
    assertNull(fixture.status());
    assertFalse(fixture.assertion());
    assertTrue(fixture.trouble());
  }

  @Test
  public void anErrorWithAnExpectedValueStillCounts() {
    SlimTestResult error = new SlimTestResult("actual", "expected", "boom", ExecutionResult.ERROR);
    CellRecord cell = CellRecord.from(error, "expected");
    assertEquals("error", cell.status());
    assertTrue(cell.assertion());
    assertEquals("expected", cell.expected());
  }

  @Test
  public void aTableProblemIsNotAnAssertion() {
    CellRecord problem = CellRecord.error(new SlimExceptionResult("id",
      EXCEPTION_TAG + "message:<<NO_CLASS agnesse.missing.Fixture>>")).asTableProblem();
    assertFalse(problem.assertion());
    assertTrue(problem.trouble());
    assertNull(problem.status());
  }

  @Test
  public void aSymbolIsRecordedOnlyWhenTheMessageIsClosed() {
    CellRecord symbol = CellRecord.from(SlimTestResult.pass("$total<-[3]"), "3");
    assertEquals("total", symbol.symbol());
    assertEquals("3", symbol.value());
    assertTrue(symbol.dump().contains("\"symbol\":\"total\""));

    CellRecord open = CellRecord.from(SlimTestResult.pass("$total<-[3"), "3");
    assertNull(open.symbol());
    assertFalse(open.dump().contains("\"symbol\""));
    assertNull(CellRecord.from(SlimTestResult.pass("hello<-[3]"), "3").symbol());
  }

  @Test
  public void aResultThatIsNotSlimCarriesNoStatus() {
    CellRecord cell = CellRecord.from(new TestResult() {
      public boolean doesCount() { return true; }
      public boolean hasActual() { return false; }
      public String getActual() { return null; }
      public boolean hasExpected() { return false; }
      public String getExpected() { return null; }
      public boolean hasMessage() { return false; }
      public String getMessage() { return null; }
      public ExecutionResult getExecutionResult() { return ExecutionResult.PASS; }
      public Map<String, ?> getVariablesToStore() { return Map.of(); }
    });
    assertNull(cell.status());
    assertFalse(cell.assertion());
  }
}
