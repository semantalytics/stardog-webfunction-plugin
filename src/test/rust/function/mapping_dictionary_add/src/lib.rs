use std::ffi::{CStr, CString}; 
use std::os::raw::c_char;
use serde_json::{Value, json};

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let values: Value = serde_json::from_str(subject).unwrap();
    let value_0 = values["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap();

    let result = value_0.to_uppercase();

    let mapping_dictionary_add_value = json!({
      "head": {"vars":["value_0"]}, "results":{"bindings":[{"value_0":{"type":"literal","value": result}}]}
    }).to_string().into_bytes();

    let mapping_dictionary_add_ptr = unsafe { CString::from_vec_unchecked(mapping_dictionary_add_value) }.into_raw();

    let id = unsafe { mapping_dictionary_add(mapping_dictionary_add_ptr as i32) };

    let result = json!({
      "head": {"vars":["value_0"]}, "results":{"bindings":[{"value_0":{"type":"literal","value": format!("[{}]", id), "datatype": "tag:stardog:api:array"}}]}
    }).to_string().into_bytes();

    unsafe { CString::from_vec_unchecked(result) }.into_raw()
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