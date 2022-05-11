use std::ffi::CString;
use std::os::raw::c_char;
use serde_json::json;

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(_subject: *mut c_char) -> *mut c_char {

    let result = json!({
      "head": {"vars":["value_0", "value_1"]}, "results":{"bindings":[
            {"value_0":{"type":"literal","value": "contant0"}, "value_1":{"type":"literal","value": "contant1"}}
        ]}
    }).to_string().into_bytes();

    unsafe { CString::from_vec_unchecked(result) }.into_raw()

}
