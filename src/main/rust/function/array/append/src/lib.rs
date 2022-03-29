use std::ffi::{CStr, CString}; 
use std::os::raw::c_char;
use serde_json::{Value, json};

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let values: Value = serde_json::from_str(subject).unwrap();
    let value_1 = values["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap();
    let datatype_1 = values["results"]["bindings"][0]["value_1"]["datatype"].as_str().unwrap();
    let value_2 = values["results"]["bindings"][0]["value_2"]["value"].as_str().unwrap();

    //if value_1 is not array literal create a new array literal containing value_1 and value_2
    //if value_1 is an array literal append value_2
    //?? should ther be a separate function to concatenate two array literals? flatConcat?

    let sparql_query_result = json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": "THE_RESULT"}}]}
    }).to_string().into_bytes();

    unsafe { CString::from_vec_unchecked(sparql_query_result) }.into_raw()

}

#[no_mangle]
pub extern fn doc() -> *mut c_char {

    let output = b"
function description here

arguemnts:
    value_0:literal first string to compare
    value_1:literal second string to compare

	".to_vec();

    unsafe { CString::from_vec_unchecked(output) }.into_raw()
}