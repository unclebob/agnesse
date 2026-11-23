{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A static page does not run `SetUp` or `TearDown`. A test page does, and a nearer one replaces the one above it.

| Library |
| --- |
| echo fixture |
| page driver |

| script | | | |
| --- | --- | --- | --- |
| given page | SetupNest.SetUp | with content | nest-setup |
| given page | SetupNest.TearDown | with content | nest-teardown |
| given page | SetupNest.Plain | with content | nest-plain |
| check | request page | SetupNest.Plain | 200 |
| reject | content contains | nest-setup |
| reject | content contains | nest-teardown |
| given page | SetupNest.Checked | with content | nest-checked |
| make | SetupNest.Checked | a test page |
| check | request page | SetupNest.Checked | 200 |
| ensure | content contains | nest-setup |
| ensure | content contains | nest-checked |
| ensure | content contains | nest-teardown |
