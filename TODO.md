# TODOs

## Chores
- Create a new PGP key for Sonatype 
- Register the library with Scaladex
- https://github.com/VirtusLab/scala-steward-repos/pull/676 after the https://github.com/scala-steward-org/scala-steward/issues/3945
- Fix Scaladoc linker warnings in ci-release action
- Update example schedules in examples/
- 

## Features
- Detection of non-existing days (i.e. `31` in `April` and `30` in `February`).
  Should there be warnings for `Schedule.in(January to December).on(31.th).at(time"8:00")`?
  And what about `Schedule.in(January to March).on(30.th).at(time"8:00")`?
- `except` syntax: `All except Mondays` or `Monday to Friday except (Wednesdays, Thursdays)` and similar for the other cron fields.
