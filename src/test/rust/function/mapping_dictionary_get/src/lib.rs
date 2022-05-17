use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use serde_json::{Value, json};

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let v: Value = serde_json::from_str(subject).unwrap();

    let result:Vec<&str> = v["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap().trim_matches(|c| c == '[' || c == ']').split(",").collect();

    return unsafe { mapping_dictionary_get(result[0].trim().parse::<i64>().unwrap()) };
}

#[no_mangle]
pub extern fn cardinality_estimate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let values: Value = serde_json::from_str(subject).unwrap();
    let estimate = values["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap();

    let result = json!({
      "head": {"vars":["value_0", "value_1"]}, "results":{"bindings":[
            {"value_0":{"type":"literal","value": estimate}, "value_1":{"type":"literal","value": "ACCURATE"}}
        ]}
    }).to_string().into_bytes();

    unsafe { CString::from_vec_unchecked(result) }.into_raw()
}