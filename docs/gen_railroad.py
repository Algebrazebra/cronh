#!/usr/bin/env python3
"""Generate railroad diagrams for the cronh Schedule DSL.

Renders one diagram per grammar rule (bottlecaps-style) into a single
self-contained HTML page and a combined SVG. Run from the repo root:

    python3 docs/gen_railroad.py

Requires `railroad-diagrams` (pip install railroad-diagrams).
"""
import io
import re
import os

from railroad import (
    Diagram,
    Sequence,
    Choice,
    Optional,
    OneOrMore,
    Terminal,
    NonTerminal,
    Comment,
)

try:
    from railroad import DEFAULT_STYLE
except ImportError:  # older versions
    DEFAULT_STYLE = ""

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- value leaves reused below -------------------------------------------

def commalist(item, sep=","):
    """item ( ',' item )*"""
    return OneOrMore(item, Terminal(sep))

# ---- one (name, Diagram) per grammar rule, in reading order --------------

RULES = []

def rule(name, *items):
    RULES.append((name, Diagram(*items, type="simple")))

rule(
    "schedule",
    Terminal("Schedule"),
    Choice(
        0,
        Sequence(
            Optional(NonTerminal("months"), skip=True),
            Optional(NonTerminal("day"), skip=True),
            Optional(NonTerminal("time"), skip=True),
        ),
        NonTerminal("preset"),
    ),
)

rule(
    "preset",
    Terminal("Schedule"),
    Choice(
        0,
        Terminal(".daily"),
        Terminal(".hourly"),
        Terminal(".monthly"),
        Terminal(".yearly"),
        Terminal(".weekdays"),
        Terminal(".weekends"),
        Terminal(".everyMinute"),
    ),
)

rule(
    "months",
    Terminal(".in("),
    commalist(NonTerminal("month")),
    Terminal(")"),
)

rule(
    "day",
    Choice(
        0,
        Sequence(Terminal(".on("), commalist(NonTerminal("weekday")), Terminal(")")),
        Sequence(Terminal(".on("), NonTerminal("weekdaySelector"), Terminal(")")),
        Sequence(Terminal(".onThe("), commalist(NonTerminal("monthDay")), Terminal(")")),
    ),
)

rule(
    "time",
    Choice(
        0,
        Sequence(
            Terminal(".at("),
            NonTerminal("hour"),
            Optional(Sequence(Terminal(","), NonTerminal("minute")), skip=True),
            Terminal(")"),
        ),
        Sequence(Terminal(".at("), NonTerminal("minute"), Terminal(")")),
        Sequence(
            NonTerminal("hourRange"),
            Optional(
                Sequence(Terminal(".at("), NonTerminal("minute"), Terminal(")")),
                skip=True,
            ),
        ),
        Terminal(".everyMinute"),
    ),
)

rule(
    "hourRange",
    Choice(
        0,
        Sequence(
            Terminal(".between("),
            NonTerminal("hour"),
            Terminal(","),
            NonTerminal("hour"),
            Terminal(")"),
        ),
        Terminal(".everyHour"),
    ),
)

rule(
    "weekdaySelector",
    Choice(
        0,
        Terminal("Weekdays"),
        Terminal("Weekends"),
        Sequence(NonTerminal("weekday"), Terminal("to"), NonTerminal("weekday")),
        Sequence(
            Terminal("WeekdaySelector("),
            commalist(NonTerminal("weekday")),
            Terminal(")"),
        ),
    ),
)

rule(
    "weekday",
    Choice(
        0,
        Terminal("Mon"),
        Terminal("Tue"),
        Terminal("Wed"),
        Terminal("Thu"),
        Terminal("Fri"),
        Terminal("Sat"),
        Terminal("Sun"),
    ),
)

rule("month", Comment("Month.January  …  Month.December"))
rule("monthDay", Sequence(Comment("1 … 31"), Terminal(".dom")))
rule(
    "hour",
    Choice(
        0,
        Sequence(Comment("0 … 23"), Terminal(".h")),
        Terminal("midnight"),
        Terminal("noon"),
    ),
)
rule("minute", Sequence(Comment("0 … 59"), Terminal(".m")))


def svg_of(diagram):
    buf = io.StringIO()
    diagram.writeSvg(buf.write)
    return buf.getvalue()


EBNF = open(os.path.join(HERE, "dsl-grammar.ebnf")).read()

# ---- self-contained HTML page -------------------------------------------

parts = [
    "<!DOCTYPE html><html><head><meta charset='utf-8'>",
    "<title>cronh Schedule DSL — railroad diagram</title>",
    "<style>",
    DEFAULT_STYLE,
    "body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;margin:2rem;"
    "max-width:60rem}h1{font-size:1.4rem}h2{font-size:1rem;margin:1.6rem 0 .2rem;"
    "color:#1a5}pre{background:#f6f8fa;padding:1rem;border-radius:6px;"
    "overflow:auto;font-size:.85rem}svg.railroad-diagram{background:#fff}"
    "p.note{color:#444;font-size:.9rem}",
    "</style></head><body>",
    "<h1>cronh <code>Schedule</code> DSL — railroad diagram</h1>",
    "<p class='note'>Left-to-right flow is the coarse&rarr;fine type-state: "
    "<code>months</code> &rarr; <code>day</code> &rarr; <code>time</code>, each "
    "optional, never backwards. The grammar shows allowed orderings; the phantom "
    "types additionally forbid setting a field twice and combining "
    "<code>.on</code> with <code>.onThe</code>. Presets are entry shortcuts for "
    "particular states (e.g. <code>.weekdays</code> &equiv; "
    "<code>.on(Mon to Fri)</code>) and continue with the finer segments.</p>",
]
for name, diagram in RULES:
    parts.append(f"<h2>{name}</h2>")
    parts.append(svg_of(diagram))
parts.append("<h2>EBNF</h2><pre>")
parts.append(EBNF.replace("&", "&amp;").replace("<", "&lt;"))
parts.append("</pre>")
parts.append("</body></html>")

with open(os.path.join(HERE, "dsl-railroad.html"), "w") as f:
    f.write("\n".join(parts))

# ---- combined standalone SVG (rules stacked vertically) ------------------

PAD_X, LABEL_H, GAP = 8, 26, 18
body = []
y = 10
maxw = 0
for name, diagram in RULES:
    s = svg_of(diagram)
    w = int(round(float(re.search(r'width="([\d.]+)"', s).group(1))))
    h = int(round(float(re.search(r'height="([\d.]+)"', s).group(1))))
    inner = re.sub(r"^<svg[^>]*>", "", s).rsplit("</svg>", 1)[0]
    body.append(
        f'<text x="{PAD_X}" y="{y + 16}" class="rr-label">{name}</text>'
        f'<g class="railroad-diagram" transform="translate({PAD_X},{y + LABEL_H})">'
        f"{inner}</g>"
    )
    y += LABEL_H + h + GAP
    maxw = max(maxw, w + 2 * PAD_X)

# In a combined SVG the inner diagrams are <g>, not <svg>, so retarget the
# library's `svg.railroad-diagram ...` selectors at the `.railroad-diagram` class.
combined_css = DEFAULT_STYLE.replace("svg.railroad-diagram", ".railroad-diagram")

svg = (
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{maxw}" height="{y}" '
    f'viewBox="0 0 {maxw} {y}">'
    f"<style>{combined_css} text.rr-label{{font:bold 15px sans-serif;fill:#1a5}}"
    f"svg{{background:#fff}}</style>"
    f'<rect width="{maxw}" height="{y}" fill="#fff"/>'
    + "".join(body)
    + "</svg>"
)
with open(os.path.join(HERE, "dsl-railroad.svg"), "w") as f:
    f.write(svg)

print(f"wrote dsl-railroad.html and dsl-railroad.svg ({len(RULES)} rules)")
