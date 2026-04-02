# IFSX GUI User Guide

Graphical tool for extracting, repacking, and inspecting QNX IFS boot images.

![screenshot](screenshot.png)

## Prerequisites

IFSX requires `dumpifs` and `mkifs` from the QNX Software Development Platform (SDP).
Source the SDP environment script before launching the application:

```
source ~/qnx800/qnxsdp-env.sh
```

If either tool is not found at startup, a warning dialog is shown:

> Warning: dumpifs and/or mkifs not found. Did you source the SDP?

There is no in-app configuration for tool paths. The tools must be on `PATH`.

## Interface overview

The main window has three tabs: **Extract**, **Pack**, and **Inspect**.
A status bar at the bottom shows the result of the last operation.

---

## Extract tab

Extracts a QNX IFS image into a self-contained directory.

1. Set **IFS File** — type a path, use **Browse...**, or drag and drop an IFS file onto the field.
2. Set **Output Directory** — type a path or use **Browse...**. When you select an IFS file, the
   output directory is suggested automatically as `<name>_extracted/` next to the image.
3. Optionally select hooks to run (see **Hooks** below).
4. Click **Extract**.

A progress bar and log area appear while the operation runs.
On success, the output directory contains:
- the extracted filesystem tree
- `_ifsx.build` — buildfile used for repacking
- `_ifsx.meta` — image metadata

---

## Pack tab

Repacks an extracted directory back into an IFS image.

1. Set **Source Directory** — type a path, use **Browse...**, or drag and drop a directory onto the
   field. The directory must contain `_ifsx.build`.
2. Set **Output IFS** — type a path or use **Browse...**. A default name (`<dir>.ifs`) is suggested
   when you select the source directory.
3. Optionally select hooks to run (see **Hooks** below).
4. Click **Pack**.

A progress bar and log area appear while the operation runs.

---

## Inspect tab

Browses the contents of an IFS image without extracting it.

Open an image with **File > Open IFS for Inspection...** (`Ctrl+O`) or drag and drop an IFS file
onto the tree view.

The left panel shows the filesystem tree. Click any entry to see its details on the right:

| Field   | Description                              |
|---------|------------------------------------------|
| Type    | Entry type (file, directory, symlink...) |
| Path    | Full path inside the image               |
| Size    | File size                                |
| Offset  | Byte offset within the image             |
| Mode    | Unix permission bits                     |
| UID/GID | Owner and group                          |
| Inode   | Inode number                             |
| Target  | Symlink target (symlinks only)           |
| ELF     | ELF architecture info (executables only) |

For startup-script entries a **Script Content** section expands below the details.

---

## Hooks

IFSX supports user-defined hook executables that run before or after extract and pack operations.

### Directory structure

At startup IFSX creates `~/.ifsx/hooks/` and its four subdirectories if they do not already exist:

```
~/.ifsx/hooks/
    pre-extract/    run before extraction
    post-extract/   run after extraction
    pre-pack/       run before packing
    post-pack/      run after packing
```

Any executable file placed in one of these directories is a hook and will appear in the
corresponding tab.

### Hook arguments

Each hook is called with two positional arguments:

1. The IFS image path (absolute)
2. The directory path (absolute)

For **extract** hooks: argument 1 is the source IFS file, argument 2 is the output directory.
For **pack** hooks: argument 1 is the output IFS path, argument 2 is the extracted directory.

### Selecting hooks in the GUI

When hooks are present, checkbox lists appear in the **Extract** and **Pack** tabs:

- **Pre-extract hooks** / **Post-extract hooks** — shown in the Extract tab
- **Pre-pack hooks** / **Post-pack hooks** — shown in the Pack tab

Hooks are listed in alphabetical order. Check the ones you want to run before clicking
**Extract** or **Pack**. Selected hooks execute in alphabetical order for each phase.

### Output and error handling

Hook output (stdout and stderr combined) is streamed into the log area alongside the
tool output. If a hook exits with a non-zero code the operation stops immediately — the
error is shown in the log area and the remaining hooks in the chain are not executed.
