"""Single source of truth for the tile classes.

Order is the model's class index order and MUST match the declaration order of
`android/app/src/main/java/com/tilevision/model/Tile.kt`.
"""

CLASSES = (
    [f"{n}m" for n in range(1, 10)]
    + [f"{n}p" for n in range(1, 10)]
    + [f"{n}s" for n in range(1, 10)]
    # East, South, West, North, Haku (white), Hatsu (green), Chun (red)
    + [f"{n}z" for n in range(1, 8)]
    # Red fives
    + ["0m", "0p", "0s"]
)

assert len(CLASSES) == 37
