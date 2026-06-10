# ServerControl Networking Protocol

The Fabric mod and Paper plugin use one custom payload/plugin messaging channel:

```text
servercontrol:main
```

All primitive values are encoded in network byte order. Strings and byte arrays are encoded as `int length` followed by raw UTF-8 bytes or raw file bytes so the Fabric `PacketByteBuf` and Paper `DataInputStream` implementations stay byte-for-byte identical.

## Header

Every packet begins with:

```text
int magic = 0x5343544c // "SCTL"
int protocolVersion = 3
unsigned byte opcode
```

## Opcodes

`1` - `HELLO`, server to client during play:

```text
long nonce
int stateFlags
string serverPluginVersion
```

`2` - `HELLO_ACK`, client to server during play:

```text
long nonce
string clientModVersion
```

`3` - `STATE`, server to client during play whenever settings change:

```text
int stateFlags
```

`4` - `CLIENT_READY`, client to server during play:

```text
string clientModVersion
```

`5` - `SOUND_META`, server to client during play:

```text
string serverId
string sha256
int byteCount
int chunkCount
int volumePercent
```

`6` - `SOUND_CHUNK`, server to client during play:

```text
string sha256
int chunkIndex
byte[] mp3Data
```

`7` - `PLAY_CUSTOM_DEATH_SOUND`, server to client during play:

```text
string sha256
int volumePercent
```

## State Flags

```text
bit 0 = tab list disabled
bit 1 = public chat disabled
bit 2 = death messages disabled
bit 3 = join messages disabled
bit 4 = quit messages disabled
bit 5 = advancement messages disabled
bit 6 = armor damage enabled
bit 7 = universal death sound enabled
bit 8 = custom death sound enabled (only set when universal death sound is also enabled)
```

The client proactively sends `CLIENT_READY` after entering play. The server replies with `HELLO` containing a random nonce and requires the same nonce in `HELLO_ACK`. The server and client also compare their exact ServerControl release version strings during this handshake. A protocol mismatch or release-version mismatch is rejected, so older client jars do not work with newer server plugin jars and newer client jars do not work with older server plugin jars. Vanilla clients and Fabric clients without this mod do not answer the challenge and are disconnected after the configured timeout.

Custom MP3 death sounds are keyed by the server-generated `client-assets.server-id` plus the file SHA-256, not by IP address. If `plugins/ServerControl/sounds/death.mp3` changes while players are online, the server re-sends metadata/chunks and future death events play the replacement file. The bundled client player decodes MP3 audio; unsupported uploads such as real MP4/AAC files, or files over the 20 MB client transfer limit, fall back to the vanilla global Iron Golem sound.
