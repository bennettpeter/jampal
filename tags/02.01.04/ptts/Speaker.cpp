
/*
    Copyright 2004 Peter Bennett

    This file is part of ptts - Peter's Text to Speech and jampal

    ptts and jampal are free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; version 2 of the License.

    ptts is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ptts; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


#include <windows.h>
#include <sapi.h>
#include "ptts.h"
#include "pgbennett_speech_Speaker.h"



/*
 * Class:     Speaker
 * Method:    init
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_pgbennett_speech_Speaker_init
    (JNIEnv *env, jobject obj, jstring jsFileName) {

    const jchar *pFileName;
    if (jsFileName==0)
        pFileName=0;
    else
        pFileName = env->GetStringChars(jsFileName,0);
    int hSpeaker = createSpeaker((const wchar_t *)pFileName);
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    env->SetIntField(obj, fid, hSpeaker); 
    if (pFileName)
	    env->ReleaseStringChars(jsFileName, pFileName);
    if (hSpeaker == 0)
        return JNI_FALSE;
    return JNI_TRUE;
}


/*
 * Class:     Speaker
 * Method:    speak
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_pgbennett_speech_Speaker_speak
(JNIEnv *env, jobject obj, jstring jsText) {
    if (jsText==0)
        return JNI_FALSE;
    const jchar *pText = env->GetStringChars(jsText,0);
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    int hSpeaker = env->GetIntField(obj, fid); 
    if (hSpeaker == 0)
        return JNI_FALSE;
    int rc=Speak(hSpeaker,(const wchar_t *)pText);
	env->ReleaseStringChars(jsText, pText);
    return rc;

}

/*
 * Class:     Speaker
 * Method:    setRate
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_pgbennett_speech_Speaker_setRate
(JNIEnv *env, jobject obj, jint rate) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    int hSpeaker = env->GetIntField(obj, fid); 
    if (hSpeaker == 0)
        return JNI_FALSE;
    int rc=setRate(hSpeaker,rate);
    return rc;

}

/*
 * Class:     Speaker
 * Method:    setVolume
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_pgbennett_speech_Speaker_setVolume
(JNIEnv *env, jobject obj, jint volume) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    int hSpeaker = env->GetIntField(obj, fid); 
    if (hSpeaker == 0)
        return JNI_FALSE;
    int rc=setVolume(hSpeaker,volume);
    return rc;

}

/*
 * Class:     Speaker
 * Method:    close
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_pgbennett_speech_Speaker_close
(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    int hSpeaker = env->GetIntField(obj, fid); 
    if (hSpeaker == 0)
        return JNI_FALSE;
    int rc=closeSpeaker(hSpeaker);
    hSpeaker = 0;
    env->SetIntField(obj, fid, hSpeaker); 
    return rc;

}


/*
 * Class:     pgbennett_speech_Speaker
 * Method:    setVoice
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_pgbennett_speech_Speaker_setVoice
(JNIEnv *env, jobject obj, jstring jsVoice) {
    if (jsVoice==0)
        return;
    const jchar *pVoice = env->GetStringChars(jsVoice,0);
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "hSpeaker", "I");
    int hSpeaker = env->GetIntField(obj, fid); 
    if (hSpeaker == 0)
        return;
    int rc=setVoice(hSpeaker,(const WCHAR *)pVoice);
	env->ReleaseStringChars(jsVoice, pVoice);
	
}

 

/*
 * Class:     pgbennett_speech_Speaker
 * Method:    getVoices
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_pgbennett_speech_Speaker_getVoices
(JNIEnv *env, jclass cls) {
	jchar **ppszDescription;
	UINT count = listVoices((WCHAR ***)&ppszDescription,NULL);
	UINT ix;

	jclass stringClass = env->FindClass("java/lang/String"); 

	jobjectArray voices = env->NewObjectArray((jsize) count, 
		stringClass, NULL); 

	for (ix=0;ix < count; ix++) {
//		fwprintf(stdout,L"%s\n",ppszDescription[ix]);	
		jstring str = env->NewString(ppszDescription[ix],
			wcslen((const wchar_t *)ppszDescription[ix])); 
		env->SetObjectArrayElement(voices, ix, str);
	}
	delete [] ppszDescription;

	return voices;
}



BOOL APIENTRY DllMain(HANDLE hModule, 
                      DWORD  ul_reason_for_call, 
                      LPVOID lpReserved)
{
    switch( ul_reason_for_call ) {
    case DLL_PROCESS_ATTACH:
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
        break;
    case DLL_PROCESS_DETACH:
        closeDown();
    }
    return TRUE;
}

