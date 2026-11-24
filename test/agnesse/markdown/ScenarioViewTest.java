package agnesse.markdown;

import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageDummy;
import agnesse.wiki.fs.FileSystemPageFactory;
import agnesse.wiki.fs.MemoryFileSystem;
import agnesse.wiki.fs.WikiFiles;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScenarioViewTest {
  private MemoryFileSystem fileSystem;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    fileSystem = new MemoryFileSystem();
    fileSystem.makeFile(new File("root/_root.md"), """
      { "type": "static" }
      | scenario | secret |
      | --- |
      | note | root-secret |
      """);
    root = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
      .makePage(new File("root"), "root", null, new SystemVariableSource());
  }

  @Test
  public void aScriptCallShowsTheScenarioBodyCollapsedAndUncolored() throws Exception {
    page("Calls", """
      { "type": "test" }
      ```java
      scenarios Secret
      ```
      ```agnesse
      define Hidden
      nope
      ```
      | scenario | greet | name |
      | --- | --- | --- |
      | note | first-body |

      | scenario | greet | name |
      | --- | --- | --- |
      | note | second-body |

      | script | echo fixture |
      | --- |
      | greet | world |
      | greet | there |
      | check | echo | hi | hi |
      | Check | echo | hi | hi |
      | check not | echo | hi | no |
      | ensure | echo | hi |
      | reject | echo | no |
      | show | echo | hi |
      | note | hi |
      | start | echo fixture |
      | # comment |
      | * star |
      """);
    scenario("Calls", "check", "keyword-check");
    scenario("Calls", "Check", "keyword-Check");
    scenario("Calls", "check not", "keyword-check-not");
    scenario("Calls", "ensure", "keyword-ensure");
    scenario("Calls", "reject", "keyword-reject");
    scenario("Calls", "show", "keyword-show");
    scenario("Calls", "note", "keyword-note");
    scenario("Calls", "start", "keyword-start");
    scenario("Calls", "# comment", "keyword-hash");
    scenario("Calls", "* star", "keyword-star");
    fileSystem.makeFile(new File("root/Secret.md"), """
      { "type": "static" }
      | scenario | secret |
      | --- |
      | note | secret-step |
      """);
    fileSystem.makeFile(new File("root/Hidden.md"), """
      { "type": "static" }
      | scenario | hidden |
      | --- |
      | note | hidden-step |
      """);

    String html = html("Calls");

    assertEquals(2, count(html, "<details class=\"scenario-call\">"));
    assertTrue(html.contains("class=\"scenario-toggle\""));
    assertTrue(html.contains("class=\"when-closed\">▸"));
    assertTrue(html.contains("class=\"when-open\">▾"));
    assertTrue(html.contains("aria-expanded=\"false\""));
    assertTrue(body(html).contains("second-body"));
    assertFalse(body(html).contains("first-body"));
    assertFalse(html.contains("scenario-call pass"));
    assertFalse(html.contains("scenario-call fail"));
    assertFalse(html.contains("scenario-call error"));
    assertFalse(html.contains("<details class=\"scenario-call\" open"));
    assertFalse(html.contains("secret-step"));
    assertFalse(html.contains("hidden-step"));
    ScenarioView view = ScenarioView.collect((MarkdownPage) root.getChildPage("Calls"));
    assertNull(view.match("script", "", 0, List.of("greet", "world")));
    assertNull(view.match("script", "", 1, List.of("check", "echo", "hi", "hi")));
  }

  @Test
  public void aNestedScenarioAndASelfCallBothStop() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | greet | name |
      | --- | --- | --- |
      | note | hello @name |

      | scenario | nest |
      | --- |
      | greet | world |

      | scenario | loop |
      | --- |
      | note | once | |
      | loop |

      | script |
      | --- |
      | nest |
      | loop |
      """);

    String html = html("Calls");

    assertTrue(body(html).contains("hello @name"));
    assertTrue(html.contains("once"));
    assertTrue(count(html, "<details class=\"scenario-call\">") >= 3);
    assertTrue(count(html, "<details class=\"scenario-call\">") < 8);
  }

  @Test
  public void theHeaderRowIsNotACallEvenWhenItsNameIsAScenario() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | script |
      | --- |
      | note | not-the-header |

      | script |
      | --- |
      | script |
      """);

    assertEquals(1, count(html("Calls"), "<details class=\"scenario-call\">"));
    assertTrue(body(html("Calls")).contains("not-the-header"));
  }

  @Test
  public void aDecisionRowShowsTheScenarioAndTheHeaderDoesNot() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | make greeting | name |
      | --- | --- | --- |
      | note | hello @name |

      | make greeting |
      | --- |
      | name |
      | world |
      """);

    String html = html("Calls");
    ScenarioView view = ScenarioView.collect((MarkdownPage) root.getChildPage("Calls"));

    assertEquals(1, count(html, "<details class=\"scenario-call\">"));
    assertTrue(body(html).contains("hello @name"));
    assertNull(view.match("decision", "make greeting", 1, List.of("name")));
    assertTrue(view.match("decision", "make greeting", 2, List.of("world")) != null);
    assertNull(view.match("query", "make greeting", 2, List.of("world")));
  }

  @Test
  public void aDecisionTableUsesTheSameNameAsTheScenario() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | ping |
      | --- |
      | note | ping-body |

      | scenario | a | mid | b |
      | --- | --- | --- |
      | note | short-word |

      | ping |
      | --- |
      | value |
      | one |

      | a b |
      | --- |
      | mid |
      | x |
      """);

    String html = html("Calls");

    assertEquals(2, count(html, "<details class=\"scenario-call\">"));
    assertTrue(body(html).contains("ping-body"));
    assertTrue(body(html).contains("short-word"));
  }

  @Test
  public void emptyCellsStayWhenTheyAreTheWholeRow() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | pad |
      | --- | --- |
      | note | | |
      |  |

      | script |
      | --- |
      | pad |
      |  |
      """);

    String html = html("Calls");

    assertTrue(body(html).contains("<td>note</td></tr>"));
    assertFalse(body(html).contains("<td>note</td><td></td>"));
    assertTrue(body(html).contains("<td></td>"));
    assertTrue(html.contains("data-slim=\"\""));
  }

  @Test
  public void aSemicolonEndsTheCalledName() throws Exception {
    page("Calls", """
      { "type": "test" }
      | scenario | do it | extra |
      | --- | --- | --- |
      | note | stopped-step |

      | script |
      | --- |
      | do it; | ignored | more |
      """);

    assertTrue(body(html("Calls")).contains("stopped-step"));
  }

  @Test
  public void aLibraryScenarioIsFoundAndANearerOneWins() throws Exception {
    fileSystem.makeFile(new File("root/ScenarioLibrary.md"), """
      { "type": "test-adjunct" }
      | scenario | greet |
      | --- |
      | note | outer-body |
      """);
    fileSystem.makeFile(new File("root/Parent.md"), "{ \"type\": \"static\" }\n");
    fileSystem.makeFile(new File("root/Parent/ScenarioLibrary.md"), """
      { "type": "test-adjunct" }
      | scenario | greet |
      | --- |
      | note | inner-body |

      | scenario | only inner |
      | --- |
      | note | inner-only |
      """);
    page("Parent/Calls", """
      { "type": "test" }
      | script |
      | --- |
      | greet |
      | only inner |
      """);

    String html = root.getChildPage("Parent").getChildPage("Calls").getHtml();

    assertTrue(body(html).contains("inner-body"));
    assertFalse(body(html).contains("outer-body"));
    assertTrue(body(html).contains("inner-only"));
  }

  @Test
  public void setUpIsUsedUnlessThePageDefinesTheScenario() throws Exception {
    fileSystem.makeFile(new File("root/SetUp.md"), """
      { "type": "test-adjunct" }
      | scenario | prepared |
      | --- |
      | note | setup-step |

      | scenario | greet |
      | --- |
      | note | setup-greet |
      """);
    page("Calls", """
      { "type": "test" }
      | scenario | greet |
      | --- |
      | note | page-greet |

      | script |
      | --- |
      | prepared |
      | greet |
      """);

    String html = html("Calls");

    assertTrue(body(html).contains("setup-step"));
    assertTrue(body(html).contains("page-greet"));
    assertFalse(body(html).contains("setup-greet"));
  }

  @Test
  public void aFalseLibrarySettingSkipsLibrariesAndTrueDoesNot() throws Exception {
    fileSystem.makeFile(new File("root/ScenarioLibrary.md"), """
      { "type": "test-adjunct" }
      | scenario | greet |
      | --- |
      | note | library-step |
      """);
    page("Off", """
      { "type": "test" }
      ```agnesse
      define INCLUDE_SCENARIO_LIBRARIES
      False
      ```
      | script |
      | --- |
      | greet |
      """);
    page("On", """
      { "type": "test" }
      ```agnesse
      define INCLUDE_SCENARIO_LIBRARIES
      true
      ```
      | script |
      | --- |
      | greet |
      """);

    assertFalse(html("Off").contains("library-step"));
    assertTrue(body(html("On")).contains("library-step"));
  }

  @Test
  public void suiteBoundariesDoNotReadSetUp() throws Exception {
    fileSystem.makeFile(new File("root/SetUp.md"), """
      { "type": "test-adjunct" }
      | scenario | hidden |
      | --- |
      | note | hidden-step |
      """);
    page("SuiteSetUp", """
      { "type": "test" }
      | script |
      | --- |
      | hidden |
      """);
    page("SuiteTearDown", """
      { "type": "test" }
      | script |
      | --- |
      | hidden |
      """);

    assertFalse(html("SuiteSetUp").contains("hidden-step"));
    assertFalse(html("SuiteTearDown").contains("hidden-step"));
    assertFalse(html("SuiteSetUp").contains("scenario-call"));
  }

  @Test
  public void includedPagesAndASelfReferenceAreReadOnce() throws Exception {
    fileSystem.makeFile(new File("root/Other.md"), """
      { "type": "static" }
      | scenario | greet |
      | --- |
      | note | from-scenarios |
      """);
    fileSystem.makeFile(new File("root/Extra.md"), """
      { "type": "static" }
      | scenario | extra |
      | --- |
      | note | from-include |
      """);
    page("Calls", """
      { "type": "test" }
      ```agnesse
      scenarios Other
      ```
      ```agnesse
      include Extra
      ```
      ```agnesse
      scenarios Calls
      ```
      ```agnesse
      scenarios
      ```
      ```agnesse
      ```
      | scenario |
      | --- |
      ```agnesse
      scenarios !!!
      ```
      ```agnesse
      scenarios MissingPage
      ```
      | scenario | own |
      | --- |
      | note | own-step |

      | script |
      | --- |
      | greet |
      | extra |
      | own |
      """);

    String html = html("Calls");

    assertTrue(body(html).contains("from-scenarios"));
    assertTrue(body(html).contains("from-include"));
    assertTrue(body(html).contains("own-step"));
    assertFalse(html.contains("root-secret"));
  }

  @Test
  public void aStaticPageDoesNotCollectScenarios() throws Exception {
    page("Plain", """
      { "type": "static" }
      | scenario | greet | name |
      | --- | --- | --- |
      | note | hello |

      | script |
      | --- |
      | greet | world |
      """);

    String html = html("Plain");
    ScenarioView view = ScenarioView.collect((MarkdownPage) root.getChildPage("Plain"));

    assertFalse(html.contains("scenario-call"));
    assertNull(view.match("script", "", 1, List.of("greet", "world")));
    assertNull(ScenarioView.collect(null).match("script", "", 1, List.of("greet")));
    assertNull(view.match("script", "", 1, List.of()));
    assertNull(view.match("script", "", 1, java.util.Arrays.asList((String) null)));
    assertNull(view.match("query", "greet", 3, List.of("world")));
  }

  @Test
  public void aNonMarkdownPageIsSkipped() throws Exception {
    page("Calls", "{ \"type\": \"test\" }\n| script |\n| --- |\n| greet |\n");
    MarkdownPage page = (MarkdownPage) root.getChildPage("Calls");
    ScenarioView view = ScenarioView.empty();

    view.take(new WikiPageDummy("Other", "| scenario | greet |\n| --- |\n| note | dummy-step |\n", null), page);
    assertNull(view.match("script", "", 1, List.of("greet")));

    page("Defined", """
      { "type": "test" }
      | scenario | greet |
      | --- |
      | note | taken-step |
      """);
    view.take(root.getChildPage("Defined"), page);
    assertNotNull(view.match("script", "", 1, List.of("greet")));
  }

  private void page(String path, String content) throws Exception {
    fileSystem.makeFile(new File("root/" + path + ".md"), content);
  }

  private void scenario(String path, String name, String step) throws Exception {
    String file = "root/" + path + ".md";
    String content = fileSystem.getContent(new File(file));
    fileSystem.makeFile(new File(file), content + """

      | scenario | %s |
      | --- |
      | note | %s |
      """.formatted(name, step));
  }

  private String html(String path) {
    WikiPage page = root;
    for (String name : path.split("/")) {
      page = page.getChildPage(name);
    }
    return page.getHtml();
  }

  private static String body(String html) {
    int at = html.indexOf("class=\"scenario-body\"");
    return at < 0 ? "" : html.substring(at);
  }

  private static int count(String html, String needle) {
    int found = 0;
    int from = 0;
    while (true) {
      int at = html.indexOf(needle, from);
      if (at < 0) {
        return found;
      }
      found++;
      from = at + needle.length();
    }
  }
}
