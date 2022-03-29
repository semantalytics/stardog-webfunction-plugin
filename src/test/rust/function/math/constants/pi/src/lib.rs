use std::ffi::CString;
use std::os::raw::c_char;
use serde_json::json;
use std::f64::consts;

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(_subject: *mut c_char) -> *mut c_char {

    let result = json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": consts::PI}}]}
    }).to_string().into_bytes();

    return unsafe { CString::from_vec_unchecked(result) }.into_raw();
}
