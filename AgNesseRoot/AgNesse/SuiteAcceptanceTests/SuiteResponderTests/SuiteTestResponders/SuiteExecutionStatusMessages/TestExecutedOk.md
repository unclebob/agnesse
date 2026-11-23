{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A test that passes reports one right and no wrong.

| Library |
| --- |
| echo fixture |
| page driver |

| script | | | |
| --- | --- | --- | --- |
| given page | StatusOk | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| Import \|<br>\| --- \|<br>\| agnesse.fixtures \|<br><br>\| script \| echo fixture \|<br>\| --- \|<br>\| check \| echo \| ok \| ok \| |
| make | StatusOk | a test page |
| check | request page | StatusOk?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
