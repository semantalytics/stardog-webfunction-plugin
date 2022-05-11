use std::ffi::CStr;
use std::os::raw::c_char;
use serde_json::Value;

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let v: Value = serde_json::from_str(subject).unwrap();

    let result:Vec<&str> = v["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap().trim_matches(|c| c == '[' || c == ']').split(",").collect();

    return unsafe { mappingDictionaryGet(result[0].trim().parse::<i64>().unwrap()) };
}
