# Bits and Chisels 2.7.3 -> Minecraft 26.2 Fabric Port

## Target
- Minecraft: 26.2
- Fabric Loader: 0.19.3
- Fabric API: 0.156.0+26.2
- Fabric Loom: 1.17-SNAPSHOT
- Java: 25
- Port version: 2.7.3+26.2-port.1

## Ported core features
- 16x16x16 (4096) micro-bit storage per Bits Block
- Full-block conversion to chiseled bit blocks
- Single-bit diamond chisel behavior
- 4x4x4 iron chisel behavior
- Smart chisel region behavior with server validation
- Bit item state storage using modern data components
- Bit placement across block faces
- Block entity save/load and client synchronization
- Dynamic collision/selection voxel shape
- Wrench rotate/mirror behavior
- Blueprint copy/place behavior
- 26.2 custom-payload networking
- 26.2 block/item/creative-tab registration
- 26.2 block entity render-state renderer (no raw OpenGL)
- 26.2 item-model JSON layout
- 26.2 recipe directory/result syntax
- Original textures and translations retained

## Intentionally deferred from the old build
These optional/visual compatibility layers were removed from the base port so they do not prevent 26.2 from loading:
- old REI integration
- Canvas/FREX compatibility hooks
- DashLoader integration
- Flan/GOML compatibility hooks
- old mixin-based red selection-box renderer
- old particle suppression mixin

The base renderer is correctness-first and renders visible micro-bits through the 26.2 block-model render-state API. It can be optimized with mesh batching after runtime validation.

## Verification state
- Resource JSON syntax: validated
- No legacy intermediary `class_####` references remain in the port source
- No TODO/FIXME placeholders in the port source
- A Java 25 GitHub Actions workflow is included at `.github/workflows/build.yml`
- A real Loom compile has NOT been completed in this sandbox because its installed JDK is Java 21 and external Java-25/Gradle dependency downloads are blocked in the execution environment.

Do not publish or label a JAR as production-ready until the included Java 25 workflow passes and a client/server smoke test is performed.
