//! LanguageTool on the user's sync server: the `languagetool` service, which forwards to the
//! LanguageTool server its admin set up. The manifest names it under `permissions.hammerServer`:
//!
//! ```toml
//! [permissions]
//! hammerServer = ["languagetool"]
//! ```
//!
//! ```ignore
//! use hammer::server::languagetool::{self, CheckRequest};
//!
//! let checked = languagetool::check(&CheckRequest::new(text, "en-US"))?;
//! for found in &checked.matches {
//!     if let Some(range) = found.byte_range(text) {
//!         hammer::log::info(&format!("{}: {}", &text[range], found.message));
//!     }
//! }
//! ```

use super::{ErrorKind, Response, ServerError};
use serde::Deserialize;
use serde::de::DeserializeOwned;
use std::ops::Range;

pub const SERVICE: &str = "languagetool";
pub const CHECK_PATH: &str = "/v2/check";
pub const LANGUAGES_PATH: &str = "/v2/languages";

/// The most a check's encoded form may be on a sync server left at its defaults; an admin may allow
/// more. Encoding can triple text that isn't ASCII, so keep a check's text to about 30,000 bytes.
pub const DEFAULT_MAX_REQUEST_BYTES: usize = 100_000;

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub enum Level {
    #[default]
    Default,
    /// More checks of style and typography.
    Picky,
}

/// A check of [CheckRequest::text]. Rules and categories are LanguageTool's ids, such as
/// `EN_A_VS_AN` or `TYPOS`.
#[derive(Debug, Clone, Default, PartialEq)]
pub struct CheckRequest {
    pub text: String,
    /// A language code such as `en-US`, or `auto` for LanguageTool to detect it.
    pub language: String,
    /// The writer's first language, for rules about false friends.
    pub mother_tongue: Option<String>,
    /// With `auto`, the variants to prefer, such as `en-GB` and `de-AT`.
    pub preferred_variants: Vec<String>,
    pub enabled_rules: Vec<String>,
    pub disabled_rules: Vec<String>,
    pub enabled_categories: Vec<String>,
    pub disabled_categories: Vec<String>,
    /// Only the enabled rules and categories, none of the rest.
    pub enabled_only: bool,
    pub level: Level,
}

impl CheckRequest {
    pub fn new(text: impl Into<String>, language: impl Into<String>) -> CheckRequest {
        CheckRequest { text: text.into(), language: language.into(), ..CheckRequest::default() }
    }

    /// The form LanguageTool reads, URL-encoded.
    pub fn form(&self) -> String {
        let language = match self.language.trim() {
            "" => "auto",
            language => language,
        };
        let lists = [
            ("preferredVariants", &self.preferred_variants),
            ("enabledRules", &self.enabled_rules),
            ("disabledRules", &self.disabled_rules),
            ("enabledCategories", &self.enabled_categories),
            ("disabledCategories", &self.disabled_categories),
        ]
        .map(|(name, values)| (name, values.join(",")));
        let mut fields = vec![("text", self.text.as_str()), ("language", language)];
        if let Some(mother_tongue) = &self.mother_tongue {
            fields.push(("motherTongue", mother_tongue));
        }
        fields.extend(lists.iter().filter(|(_, value)| !value.is_empty()).map(|(name, value)| (*name, value.as_str())));
        if self.enabled_only {
            fields.push(("enabledOnly", "true"));
        }
        if self.level == Level::Picky {
            fields.push(("level", "picky"));
        }
        crate::http::form(&fields)
    }
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CheckResponse {
    #[serde(default)]
    pub language: Option<CheckedLanguage>,
    #[serde(default)]
    pub matches: Vec<Match>,
    #[serde(default)]
    pub warnings: Warnings,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Warnings {
    /// LanguageTool stopped before the end of the text, as when a check takes too long.
    #[serde(default)]
    pub incomplete_results: bool,
}

/// The language the text was checked in, and the one LanguageTool detected in it.
#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CheckedLanguage {
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub code: String,
    #[serde(default)]
    pub detected_language: Option<DetectedLanguage>,
}

#[derive(Debug, Clone, Default, Deserialize)]
pub struct DetectedLanguage {
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub code: String,
    #[serde(default)]
    pub confidence: Option<f64>,
}

/// A problem LanguageTool found. [offset](Match::offset) and [length](Match::length) count UTF-16
/// units of the text checked, as Java does; [Match::byte_range] has them as bytes.
#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Match {
    #[serde(default)]
    pub message: String,
    #[serde(default)]
    pub short_message: String,
    pub offset: usize,
    pub length: usize,
    #[serde(default)]
    pub replacements: Vec<Replacement>,
    #[serde(default)]
    pub context: Context,
    #[serde(default)]
    pub sentence: String,
    #[serde(default)]
    pub rule: Rule,
}

impl Match {
    /// The match's bytes in `text`, the text that was checked; None when it runs past the end, or
    /// starts or ends inside a character.
    pub fn byte_range(&self, text: &str) -> Option<Range<usize>> {
        let (start, end) = (self.offset, self.offset.checked_add(self.length)?);
        let mut units = 0;
        let (mut start_byte, mut end_byte) = (None, None);
        for (at, char) in text.char_indices().chain(std::iter::once((text.len(), '\0'))) {
            if units == start {
                start_byte = Some(at);
            }
            if units == end {
                end_byte = Some(at);
                break;
            }
            if units > end {
                break;
            }
            units += char.len_utf16();
        }
        Some(start_byte?..end_byte?)
    }
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Replacement {
    pub value: String,
    #[serde(default)]
    pub short_description: Option<String>,
}

/// Some of the text around a match, with the match's place in it in UTF-16 units.
#[derive(Debug, Clone, Default, Deserialize)]
pub struct Context {
    #[serde(default)]
    pub text: String,
    #[serde(default)]
    pub offset: usize,
    #[serde(default)]
    pub length: usize,
}

#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Rule {
    #[serde(default)]
    pub id: String,
    #[serde(default)]
    pub sub_id: Option<String>,
    #[serde(default)]
    pub description: String,
    /// Such as `misspelling`, `grammar`, `style`, `typographical`, or `register`.
    #[serde(default)]
    pub issue_type: String,
    #[serde(default)]
    pub category: Category,
    #[serde(default)]
    pub urls: Vec<RuleUrl>,
}

#[derive(Debug, Clone, Default, Deserialize)]
pub struct Category {
    /// Such as `TYPOS`, `GRAMMAR`, `STYLE`, or `REDUNDANCY`.
    #[serde(default)]
    pub id: String,
    #[serde(default)]
    pub name: String,
}

/// A page explaining the rule.
#[derive(Debug, Clone, Default, Deserialize)]
pub struct RuleUrl {
    pub value: String,
}

/// A language the server checks.
#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Language {
    pub name: String,
    pub code: String,
    /// The code with its variant, such as `en-US`, which a check takes as its language.
    pub long_code: String,
}

/// Checks `request`'s text on the sync server's LanguageTool.
pub fn check(request: &CheckRequest) -> Result<CheckResponse, ServerError> {
    let headers = [("Content-Type", "application/x-www-form-urlencoded"), ("Accept", "application/json")];
    parse(super::request(SERVICE, "POST", CHECK_PATH, &headers, Some(request.form().as_bytes()))?)
}

/// The languages the sync server's LanguageTool checks.
pub fn languages() -> Result<Vec<Language>, ServerError> {
    parse(super::request(SERVICE, "GET", LANGUAGES_PATH, &[("Accept", "application/json")], None)?)
}

fn parse<T: DeserializeOwned>(response: Response) -> Result<T, ServerError> {
    if let Some(error) = response.error() {
        return Err(error);
    }
    serde_json::from_str(&response.body).map_err(|e| ServerError {
        kind: ErrorKind::Invalid,
        message: format!("LanguageTool's reply couldn't be read: {e}"),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_plain_request_sends_only_text_and_language() {
        assert_eq!(CheckRequest::new("It are fine.", "en-US").form(), "text=It+are+fine.&language=en-US");
        assert_eq!(CheckRequest::new("Hi", " ").form(), "text=Hi&language=auto");
    }

    #[test]
    fn options_join_their_lists_and_skip_the_empty_ones() {
        let request = CheckRequest {
            mother_tongue: Some("de".into()),
            preferred_variants: vec!["en-GB".into(), "de-AT".into()],
            disabled_rules: vec!["WHITESPACE_RULE".into()],
            enabled_only: true,
            level: Level::Picky,
            ..CheckRequest::new("Hi", "auto")
        };
        assert_eq!(
            request.form(),
            "text=Hi&language=auto&motherTongue=de&preferredVariants=en-GB%2Cde-AT&disabledRules=WHITESPACE_RULE&enabledOnly=true&level=picky"
        );
    }

    #[test]
    fn a_reply_reads_into_matches() {
        let body = r#"{
            "software": {"name": "LanguageTool", "version": "6.8"},
            "warnings": {"incompleteResults": false},
            "language": {"name": "English (US)", "code": "en-US",
                "detectedLanguage": {"name": "English (US)", "code": "en-US", "confidence": 0.98}},
            "matches": [{
                "message": "Did you mean \"is\"?", "shortMessage": "Agreement",
                "replacements": [{"value": "is"}, {"value": "was", "shortDescription": "past"}],
                "offset": 3, "length": 3,
                "context": {"text": "It are fine.", "offset": 3, "length": 3},
                "sentence": "It are fine.",
                "type": {"typeName": "Other"},
                "rule": {"id": "IT_VBZ", "subId": "1", "description": "Agreement", "issueType": "grammar",
                    "category": {"id": "GRAMMAR", "name": "Grammar"},
                    "urls": [{"value": "https://example.com/rule"}]},
                "ignoreForIncompleteSentence": false, "contextForSureMatch": 0
            }]
        }"#;
        let checked: CheckResponse = parse(Response { status: 200, body: body.into() }).unwrap();
        assert_eq!(checked.language.as_ref().unwrap().detected_language.as_ref().unwrap().confidence, Some(0.98));
        let found = &checked.matches[0];
        assert_eq!((found.offset, found.length, found.short_message.as_str()), (3, 3, "Agreement"));
        assert_eq!(found.replacements[1].short_description.as_deref(), Some("past"));
        assert_eq!((found.rule.id.as_str(), found.rule.issue_type.as_str(), found.rule.category.id.as_str()), ("IT_VBZ", "grammar", "GRAMMAR"));
        assert_eq!(found.rule.urls[0].value, "https://example.com/rule");
        assert!(!checked.warnings.incomplete_results);
    }

    #[test]
    fn a_refusal_or_an_unreadable_reply_is_an_error() {
        let limited = parse::<CheckResponse>(Response { status: 429, body: String::new() }).unwrap_err();
        assert_eq!(limited.kind, ErrorKind::RateLimited);
        let garbled = parse::<CheckResponse>(Response { status: 200, body: "<html>".into() }).unwrap_err();
        assert_eq!(garbled.kind, ErrorKind::Invalid);
    }

    #[test]
    fn languages_read_their_long_codes() {
        let body = r#"[{"name": "German (Austria)", "code": "de", "longCode": "de-AT"}]"#;
        let languages: Vec<Language> = parse(Response { status: 200, body: body.into() }).unwrap();
        assert_eq!(languages[0].long_code, "de-AT");
    }

    #[test]
    fn utf16_offsets_become_byte_ranges() {
        let text = "😀 It are.";
        let found = Match { offset: 6, length: 3, ..Match::default() };
        assert_eq!(&text[found.byte_range(text).unwrap()], "are");
        let to_the_end = Match { offset: 10, length: 0, ..Match::default() };
        assert_eq!(to_the_end.byte_range(text), Some(text.len()..text.len()));
        assert_eq!(Match { offset: 1, length: 1, ..Match::default() }.byte_range(text), None, "inside the emoji");
        assert_eq!(Match { offset: 9, length: 5, ..Match::default() }.byte_range(text), None, "past the end");
    }
}
