{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
If a `SuiteSetUp` or `SuiteTearDown` page is executed without errors, it is a passing page. The run summary counts assertions, so the two `ensure` checks are the two rights.

| Library |
| --- |
| page driver |
| echo fixture |

| script | page driver |
| --- | --- |
| given page | SuiteParent | with content | ${SUT_PATH}<br> |
| make a suite page | SuiteParent |
| given page | SuiteParent.SuiteSetUp | with content | |
| given page | SuiteParent.SuiteTearDown | with content | |
| given page | SuiteParent.TestOneOne | with content | \| script \| agnesse.fixtures.Pass \|<br>\| --- \|<br>\| ensure \| ok \| |
| make a test page | SuiteParent.TestOneOne |
| given page | SuiteParent.TestOneTwo | with content | \| script \| agnesse.fixtures.Pass \|<br>\| --- \|<br>\| ensure \| ok \| |
| make a test page | SuiteParent.TestOneTwo |

For normal (HTML) responses:

| script |
| --- |
| check | request page | SuiteParent?suite&format=json | 200 |
| show | content |
| ensure | content contains | 2 right, 0 wrong, 0 ignored, 0 exceptions |
| ensure | content matches | "path":"SuiteParent.SuiteSetUp".{0,300}"status":"pass" |
| ensure | content matches | "path":"SuiteParent.SuiteTearDown".{0,300}"status":"pass" |
