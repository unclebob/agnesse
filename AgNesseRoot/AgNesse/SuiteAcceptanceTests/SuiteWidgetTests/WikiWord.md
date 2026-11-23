{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
See .AgNesse.SuiteAcceptanceTests.SuiteWidgetTests.WikiWord

A wiki word is a link to a page. The word above names this page, so the link is not an offer to create it.

| script | | | |
| --- | --- |
| check | request page | AgNesse.SuiteAcceptanceTests.SuiteWidgetTests.WikiWord | 200 |
| ensure | content contains | <h1><a href="/AgNesse.SuiteAcceptanceTests.SuiteWidgetTests.WikiWord">WikiWord</a></h1> |
| reject | content contains | WikiWord?edit&amp;nonExistent=true |
