# sfpro fonts (TV only)

The TV reskin uses one neutral family, four weights. The `sfpro` flavor takes them from
`app/src/sfpro/res/font/`:

```
chora_display_regular.otf    -> SF Pro Display Regular   (400)
chora_display_medium.otf     -> SF Pro Display Medium    (500)
chora_display_semibold.otf   -> SF Pro Display Semibold  (600)
chora_display_bold.otf       -> SF Pro Display Bold      (700)
```

These files are **gitignored and personal-use only** (see `NOTICE`). The public `foss`
flavor gets its own Inter Display files from `app/src/foss/res/font/` under the same
resource names; the two sets never meet, so nothing is overridden and there is no runtime
font lookup.

Rules:

- Only the four weights above. No italic, no Text optical size (Display-only decision,
  spec §4.3).
- Do not rename the files: code references `R.font.chora_display_*` from
  `ui/theme/tv/TvFonts.kt` and must not learn flavor-specific names.
- Do not put anything except font files in `res/font/` — a README or `.gitkeep` there is an
  invalid Android resource and fails the resource merge.
- Record the source and SHA-256 of each file in `NOTICE` before shipping.

A clean checkout has no `app/src/sfpro/res/font/` directory, and `assembleSfproDebug` will
fail with unresolved `R.font.chora_display_*` references — that is deliberate: the private
flavor is not buildable without the licensed files. CI builds `foss` only.
