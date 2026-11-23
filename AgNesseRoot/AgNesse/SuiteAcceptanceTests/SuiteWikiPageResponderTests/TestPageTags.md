{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page shows each tag in the header. A page with no tags does not invent an empty tag.

| Page creator. | | | |
| --- | --- | --- | --- |
| page name | page contents | page attributes | valid? |
| TagsPage | tagged body | Suites=alpha | true |
| PlainTagsPage | plain body | | true |

| script | page driver |
| --- | --- |
| check | request page | TagsPage | 200 |
| ensure | content contains | <span class="tag">alpha</span> |
| check | request page | PlainTagsPage | 200 |
| reject | content contains | <span class="tag"> |
