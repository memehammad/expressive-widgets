# shapegen

Computes the 35 Material 3 Expressive shapes with Google's `androidx.graphics.shapes` library
(the code Compose's `MaterialShapes` uses) and writes them to `tools/shapes.json`.

`Gen.kt` holds the shape definitions copied from `androidx.compose.material3.MaterialShapes`
(Apache 2.0). To run it, compile it together with the library's `commonMain` sources from
`androidx/graphics/graphics-shapes` and the small stand-ins in `stubs/`:

    kotlinc src/*.kt stubs/*.kt Gen.kt -d out
    kotlin -cp out gen.GenKt ../shapes.json "sunny>verysunny" "cookie9>softburst" "circle>cookie12"

Then run `python3 tools/gen_shapes.py` to regenerate the Android resources.
