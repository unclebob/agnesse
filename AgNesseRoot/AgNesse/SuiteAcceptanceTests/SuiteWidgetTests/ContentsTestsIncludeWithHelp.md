{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
**Next create a child**

| script | |
| --- | --- |
| start | Page Builder |
| line | I came first! |
| attributes | Help=First Child's help |
| page | `ParentPage.FirstChild` |

**Next create a sibling**

| script | |
| --- | --- |
| start | Page Builder |
| line | I'm younger, but wiser |
| attributes | Help=Second Child's help |
| page | `ParentPage.SecondChild` |

| script | page driver |
| --- | --- |
| check | request page | ParentPage.FirstChild | 200 |
| ensure | content contains | I came first! |
| check | page has attribute | ParentPage.FirstChild | Help | true |
| ensure | source contains; | ParentPage.SecondChild | I'm younger, but wiser |
