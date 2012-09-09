
/*
    Copyright 2004 Peter Bennett

    This file is part of ptts - Peter's Text to Speech

    ptts is free software; you can redistribute it and/or modify
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


// ptts.h


int createSpeaker(const wchar_t *fileName);
int Speak(int hSpeaker, const wchar_t *pText);
int closeSpeaker(int hSpeaker);
void closeDown();
int setRate(int hSpeaker, int rate);
int setVolume(int hSpeaker, int volume);
UINT listVoices(WCHAR ***pppszReturn, ISpObjectToken ***pppVoiceToken);
int setVoice(int hSpeaker, const WCHAR *pszVoice);
