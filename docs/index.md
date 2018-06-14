---
id: "home"
title: "Home"
layout: home
---

# rpc4s

A experimentation to see if we can have some like twirp in a scala world.

## Implementation note

_This work as a todo and memo list, as well as informing visitor of what's not done in this project :)_

- We don't support protobuf at the moment

- Streaming doesn't support Protobuf in its current format

    To stream protobuf we needs to add limiter before every message. The idea is simple:
    
    Add a fixed size int (or less, depending on the supported message max size) with a known
    length, and add the size of the protobuf message in that. On the other side, read the length
    and get the correct amount of byte to decode the message. Rinse and repeat until end of stream.

- What's the story regarding Errors ? 

    For now we are living in a perfect world but we know how it goes

    For rpc it's relatively easy, as we can return an error instead of the response
    for client streaming the same but for server streaming ?
    What happens when the server start streaming and then encounters an error ?
  
    This is how Google is doing it:
    https://github.com/googleapis/googleapis/blob/master/google/rpc/status.proto

- `example` should be part of a scripted test suite for the plugin

## Docs

`bundle exec jekyll serve --baseurl /twirp-for-scala`