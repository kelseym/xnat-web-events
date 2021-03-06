
/*
 * web: validateUsername.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */
var ValidChars = "0123456789.";

function isNumeric(sText)
{
   var IsNumber=true;
   var Char;

   for (i = 0; i < sText.length && IsNumber == true; i++) 
   { 
      Char = sText.charAt(i); 
      if (ValidChars.indexOf(Char) == -1) 
         {
         IsNumber = false;
         }
   }
   return IsNumber;
}

// check to see if input is alphanumeric
function isAlphaNumeric(val)
{
  if (val.match(/^[a-zA-Z0-9]+$/))
  {
    return true;
  }
    else
  {
    return false;
  } 
}

function appendIcon(obj,icon_class,msg,styleObj){
	if(obj.appendedIcon==undefined){
		obj.appendedIcon = document.createElement("i");
		obj.appendedIcon.className = "fa "+icon_class;
		if (Object.keys(styleObj).length) {
			for (var k in styleObj) {
				obj.appendedIcon.style[k] = styleObj[k];
			}
		}
		obj.appendedIcon.style.marginLeft="5px";
		if(obj.nextSibling==null)
		{
			obj.parentNode.insertBefore(obj.appendedIcon,obj.nextSibling);
		}else{
			obj.parentNode.appendChild(obj.appendedIcon);
		}
	}

	if(msg!==undefined)obj.appendedIcon.title=msg;
}

function appendImage(obj,img_name){
	if(obj.appendedImage==undefined){
	    obj.appendedImage = document.createElement("img");
	    obj.appendedImage.style.marginLeft="5pt";
	    if(obj.nextSibling==null)
	    {
	    	obj.parentNode.insertBefore(obj.appendedImage,obj.nextSibling);
	    }else{
	    	obj.parentNode.appendChild(obj.appendedImage);
	    }
	}
	obj.appendedImage.src=serverRoot + img_name;
}

function validateUsername(obj,button_id){
	   var valid = false;
	   if (obj.value!=""){
	   	   if(isNumeric(obj.value.charAt(0))){
              xmodal.message('User Validation', 'Username cannot begin with a number.  Please modify.');
	   	      obj.focus();
	   	   }else{
	   	   	   if(obj.value.length>40){
                   xmodal.message('User Validation', 'Username cannot exceed 40 characters');
	   	   		   obj.focus();
	   	   	   }else if(isAlphaNumeric(obj.value)){
	   	   	      valid= true;
	   	   	   }else{
                   xmodal.message('User Validation', 'Username cannot contain special characters.  Please modify.');
	     	       obj.focus();
	   	   	   }
	   	   }
	   }
	   
   		if(valid){
	   	   	   if(obj.appendedImage!=undefined)appendIcon(obj,"fa-check",null,{ color: 'green' });
   			   if(button_id!=undefined)document.getElementById(button_id).disabled=false;
   		}else{
	   	   	   appendIcon(obj,"fa-asterisk","Required",{ color: '#c66' });
   			   if(button_id!=undefined)document.getElementById(button_id).disabled=true;
   		}
	   
	   return valid;
}
