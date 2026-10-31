//! Binary content, such as an exported file or an image, which operations carry as base64 text.

use serde::{Deserialize, Deserializer, Serializer};

const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

pub fn encode(bytes: &[u8]) -> String {
    let mut out = String::with_capacity(bytes.len().div_ceil(3) * 4);
    for chunk in bytes.chunks(3) {
        let n = chunk.iter().enumerate().fold(0u32, |n, (i, b)| n | (*b as u32) << (16 - 8 * i));
        for i in 0..4 {
            out.push(if i <= chunk.len() { ALPHABET[(n >> (18 - 6 * i) & 63) as usize] as char } else { '=' });
        }
    }
    out
}

const INVALID: u8 = 0xFF;

/// Each byte's six bits, or [INVALID] for one outside the alphabet.
const VALUES: [u8; 256] = {
    let mut values = [INVALID; 256];
    let mut i = 0;
    while i < 64 {
        values[ALPHABET[i] as usize] = i as u8;
        i += 1;
    }
    values
};

/// The bytes `text` holds, or none for text that is not base64: a byte outside the alphabet, `=` anywhere
/// but the end, more than two `=` or too few to fill the last group of four, or a last group of one
/// character. Padding may be left out altogether.
pub fn decode(text: &str) -> Option<Vec<u8>> {
    let padded = text.as_bytes();
    let text = padded.strip_suffix(b"==").or_else(|| padded.strip_suffix(b"=")).unwrap_or(padded);
    if text.len() % 4 == 1 || (text.len() < padded.len() && padded.len() % 4 != 0) {
        return None;
    }
    let mut out = Vec::with_capacity(text.len() * 3 / 4);
    let (mut n, mut bits) = (0u32, 0);
    for &c in text {
        let value = VALUES[c as usize];
        if value == INVALID {
            return None;
        }
        n = n << 6 | value as u32;
        bits += 6;
        if bits >= 8 {
            bits -= 8;
            out.push((n >> bits) as u8);
        }
    }
    Some(out)
}

pub fn serialize<S: Serializer>(bytes: &[u8], serializer: S) -> Result<S::Ok, S::Error> {
    serializer.serialize_str(&encode(bytes))
}

pub mod bytes {
    use super::*;

    pub fn serialize<S: Serializer>(bytes: &[u8], serializer: S) -> Result<S::Ok, S::Error> {
        super::serialize(bytes, serializer)
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(deserializer: D) -> Result<Vec<u8>, D::Error> {
        let text = String::deserialize(deserializer)?;
        decode(&text).ok_or_else(|| serde::de::Error::custom("invalid base64"))
    }
}

pub mod option {
    use super::*;

    pub fn serialize<S: Serializer>(bytes: &Option<Vec<u8>>, serializer: S) -> Result<S::Ok, S::Error> {
        match bytes {
            Some(bytes) => super::serialize(bytes, serializer),
            None => serializer.serialize_none(),
        }
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(deserializer: D) -> Result<Option<Vec<u8>>, D::Error> {
        Option::<String>::deserialize(deserializer)?
            .map(|text| decode(&text).ok_or_else(|| serde::de::Error::custom("invalid base64")))
            .transpose()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trips() {
        for bytes in [&b""[..], b"f", b"fo", b"foo", b"foob", b"fooba", b"foobar", &[0, 255, 128, 7]] {
            assert_eq!(decode(&encode(bytes)).unwrap(), bytes);
        }
        assert_eq!(encode(b"foobar"), "Zm9vYmFy");
        assert_eq!(encode(b"fo"), "Zm8=");
    }

    #[test]
    fn decodes_padded_and_unpadded() {
        for (text, bytes) in [
            ("", &b""[..]),
            ("Zg==", b"f"),
            ("Zm8=", b"fo"),
            ("Zm9v", b"foo"),
            ("Zm9vYg", b"foob"),
            ("Zm8", b"fo"),
            ("iVBORwD/", b"\x89PNG\0\xFF"),
            ("+/+/", b"\xFB\xFF\xBF"),
        ] {
            assert_eq!(decode(text).as_deref(), Some(bytes), "{text}");
        }
    }

    #[test]
    fn refuses_what_is_not_base64() {
        for text in
            ["Z", "Zg=", "Zg===", "Zm9vY", "Zm9v=", "Zm9v==", "Z=g=", "Zm 9v", "Zm9v\n", "Zm-_", "=", "==", "Zm\u{e9}"]
        {
            assert_eq!(decode(text), None, "{text:?}");
        }
    }

    #[test]
    fn round_trips_pseudo_random_bytes() {
        let mut state = 0x2545_f491_4f6c_dd1du64;
        let bytes: Vec<u8> = (0..4099)
            .map(|_| {
                state ^= state << 13;
                state ^= state >> 7;
                state ^= state << 17;
                state as u8
            })
            .collect();
        assert_eq!(decode(&encode(&bytes)).unwrap(), bytes);
    }
}
