//! Image references in Markdown. Hammer shows a project image as `![image](hammer://image/<id>)`, or
//! with the destination in angle brackets, `<hammer://image/<id>>`. The alt text means nothing: Hammer
//! shows the image's own name.

/// What every image reference starts with.
pub const PREFIX: &str = "hammer://image/";

/// The reference to the image `image_id`, `hammer://image/<id>`, for Markdown such as `![image](…)`.
pub fn reference(image_id: i32) -> String {
    format!("{PREFIX}{image_id}")
}

/// The image id a reference names: `hammer://image/3`, or `<hammer://image/3>`, is 3. None for
/// anything else, such as another scheme, an id that is empty, not all ASCII digits, or too large for
/// an `i32`.
pub fn parse_reference(reference: &str) -> Option<i32> {
    let source = reference.strip_prefix('<').and_then(|it| it.strip_suffix('>')).unwrap_or(reference);
    let digits = source.strip_prefix(PREFIX)?;
    if digits.is_empty() || !digits.bytes().all(|b| b.is_ascii_digit()) {
        return None;
    }
    digits.parse().ok()
}

/// Every image id `markdown` refers to, each once, in the order they first appear: in images, angle-bracket destinations, reference definitions
/// (`[map]: hammer://image/3`), and code alike, as Hammer counts them. A reference is
/// `hammer://image/` and its digits where no letter, digit, `_`, `+`, `.`, or `-` comes just before it
/// and no letter, digit, or `_` just after, so `hammer://image/12x` names none. An id too large for an
/// `i32` names none either.
pub fn find_ids(markdown: &str) -> Vec<i32> {
    let bytes = markdown.as_bytes();
    let mut ids = Vec::new();
    let mut from = 0;
    while let Some(found) = markdown[from..].find(PREFIX) {
        let at = from + found;
        let start = at + PREFIX.len();
        let end = start + bytes[start..].iter().take_while(|b| b.is_ascii_digit()).count();
        let bounded = (at == 0 || !is_lead(bytes[at - 1])) && (end == bytes.len() || !is_word(bytes[end]));
        if end > start && bounded {
            if let Ok(id) = markdown[start..end].parse() {
                if !ids.contains(&id) {
                    ids.push(id);
                }
            }
        }
        from = end;
    }
    ids
}

/// The image a reference names, as [`parse_reference`] reads it, with its bytes: `image.get`, which
/// the manifest must ask for. An InvalidInput error for a reference that names no image, and
/// `image.get`'s for one not in `project`.
pub fn get(project: &str, reference: &str) -> crate::Result<crate::ops::ImageContent> {
    match parse_reference(reference) {
        Some(id) => crate::ops::image::get(project, id),
        None => Err(format!("InvalidInput: Not an image reference: {reference}").into()),
    }
}

fn is_word(b: u8) -> bool {
    b.is_ascii_alphanumeric() || b == b'_'
}

fn is_lead(b: u8) -> bool {
    is_word(b) || matches!(b, b'+' | b'.' | b'-')
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_reference_names_its_image_id() {
        assert_eq!(parse_reference("hammer://image/3"), Some(3));
        assert_eq!(parse_reference("<hammer://image/3>"), Some(3));
        assert_eq!(parse_reference("hammer://image/007"), Some(7));
        assert_eq!(parse_reference(&reference(42)), Some(42));
        assert_eq!(reference(42), "hammer://image/42");
        assert_eq!(parse_reference("hammer://image/2147483647"), Some(i32::MAX));
    }

    #[test]
    fn anything_else_names_no_image() {
        for text in [
            "https://example.com/3.png", "hammer://scene/3", "hammer://image/", "hammer://image/3x",
            "hammer://image/-3", "hammer://image/+3", "hammer://image/3 ", " hammer://image/3",
            "hammer://image/99999999999", "hammer://image/2147483648", "<hammer://image/3", "hammer://image/3>",
            "<<hammer://image/3>>", "<>", "", "hammer://image/３",
        ] {
            assert_eq!(parse_reference(text), None, "{text}");
        }
    }

    #[test]
    fn finds_every_referenced_id() {
        let text = "![a](hammer://image/1)\n\nSee ![b](<hammer://image/12> \"t\") and [c]: hammer://image/7";
        assert_eq!(find_ids(text), vec![1, 12, 7]);
        assert_eq!(find_ids("hammer://image/9 hammer://image/2 hammer://image/9"), vec![9, 2]);
        assert_eq!(find_ids("```\n![x](hammer://image/5)\n```\n![y](hammer://image/5)"), vec![5]);
        assert_eq!(find_ids("A [link](hammer://image/3)."), vec![3]);
        assert_eq!(find_ids("éhammer://image/4é"), vec![4]);
        assert_eq!(find_ids("No images here."), Vec::<i32>::new());
    }

    #[test]
    fn an_id_runs_to_its_last_digit_and_the_scheme_stands_alone() {
        assert_eq!(find_ids("![a](hammer://image/12)"), vec![12]);
        assert_eq!(find_ids("hammer://image/12"), vec![12]);
        assert_eq!(find_ids("hammer://image/12.png"), vec![12]);
        for text in [
            "hammer://image/12x", "hammer://image/12_", "xhammer://image/3", "_hammer://image/3", "+hammer://image/3",
            ".hammer://image/3", "-hammer://image/3", "9hammer://image/3", "hammer://image/", "hammer://image/x",
        ] {
            assert_eq!(find_ids(text), Vec::<i32>::new(), "{text}");
        }
    }

    #[test]
    fn an_id_too_large_for_an_i32_names_no_image() {
        assert_eq!(find_ids("![a](hammer://image/3) ![b](hammer://image/99999999999)"), vec![3]);
    }
}
