{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A decision table that calls a scenario uses the actor of the last script.

| scenario | my division1 | numerator | | denominator | | expected |
| --- | --- | --- | --- | --- | --- | --- |
| setNumerator | @numerator | | | | | |
| setDenominator | @denominator | | | | | |
| check | quotient | @expected | | | | |

| script | agnesse.slim.test.Division |
| --- | --- |

| my division1 | | |
| --- | --- | --- |
| numerator | denominator | expected |
| 10 | 2 | 5.0 |
