use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::{c_char, c_void};
use serde_json::{Value, json};
use eddie::Levenshtein;

#[no_mangle]
pub extern fn allocate(size: usize) -> *mut c_void {
    let mut buffer = Vec::with_capacity(size);
    let pointer = buffer.as_mut_ptr();
    mem::forget(buffer);

    pointer as *mut c_void
}

#[no_mangle]
pub extern fn deallocate(pointer: *mut c_void, capacity: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(pointer, 0, capacity);
    }
}

#[no_mangle]
pub extern fn doc() -> *mut c_char {

	let output = b"
Compute the Levenshtein distance between two strings.

arguemnts:
    value[0]:literal first string to compare
    value[1]:literal second string to compare
	
	".to_vec();
	
	unsafe { CString::from_vec_unchecked(output) }.into_raw()
}

#[no_mangle]
pub extern fn internalEvaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };
    
    let mut output = b"".to_vec();
    let values: Value = serde_json::from_str(subject).unwrap();

    let label1 = values["results"]["bindings"][1]["value_1"]["value"].as_str().unwrap();
    let label2 = values["results"]["bindings"][2]["value_2"]["value"].as_str().unwrap();

    let jaro = Jaro::new();
    let result = jaro.distance(label1, label2);
    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": result}}]}
    }).to_string().bytes());

    unsafe { CString::from_vec_unchecked(output) }.into_raw()
}
