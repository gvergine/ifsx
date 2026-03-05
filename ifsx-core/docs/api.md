# IFSX Core API

Zero-SDP-dependency library for IFS parsing, extraction, and buildfile generation.

## Packages

model - IfsImage, IfsEntry hierarchy, metadata containers, DuplicateStrategy
parser - DumpIfsParser (state machine, dumpifs -vvvvv text -> IfsImage)
config - IfsxConfig (four-layer tool path configuration)
executor - SdpToolExecutor (ProcessBuilder wrapper)
builder - BuildfileGenerator (_ifsx.build), MetaGenerator (_ifsx.meta)
extract - IfsExtractor (end-to-end orchestrator)
