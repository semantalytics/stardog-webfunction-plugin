use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};
use std::f64::consts;

#[no_mangle]
pub extern fn malloc(size: usize) -> *mut c_void {
    let mut buffer = Vec::with_capacity(size);
    let pointer = buffer.as_mut_ptr();
    mem::forget(buffer);

    pointer as *mut c_void
}

#[no_mangle]
pub extern fn free(pointer: *mut c_void, capacity: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(pointer, 0, capacity);
    }
}

extern {
	pub fn mappingDictionaryAdd(buf_addr: i32) -> i64;
}

#[no_mangle]
pub extern fn evaluate(_subject: *mut c_char) -> *mut c_char {

    let mut output = b"".to_vec();

    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": consts::PI}}]}
    }).to_string().bytes());

    return unsafe { CString::from_vec_unchecked(output) }.into_raw();
}
