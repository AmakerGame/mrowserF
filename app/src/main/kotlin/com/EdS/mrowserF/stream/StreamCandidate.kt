package com.EdS.mrowserF.stream

import com.EdS.mrowserF.stream.MediaUrlClassifier.MediaKind

/** A media URL seen on the current page. `seq` orders by sighting. */
data class StreamCandidate(val url: String, val kind: MediaKind, val seq: Int)
