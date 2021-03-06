/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.s16.inputmethod.skeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.InflateException;
import android.view.LayoutInflater;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import com.s16.inputmethod.skeyboard.R;

public class KeyboardSwitcher implements SharedPreferences.OnSharedPreferenceChangeListener {

	protected static final String TAG = KeyboardSwitcher.class.getSimpleName();
	
    public static final int MODE_NONE = 0;
    public static final int MODE_TEXT = 1;
    public static final int MODE_SYMBOLS = 2;
    public static final int MODE_PHONE = 3;
    public static final int MODE_URL = 4;
    public static final int MODE_EMAIL = 5;
    public static final int MODE_IM = 6;
    public static final int MODE_WEB = 7;
    public static final int MODE_NUMBER = 8;

    // Main keyboard layouts without the settings key
    public static final int KEYBOARDMODE_NORMAL = R.id.mode_normal;
    public static final int KEYBOARDMODE_URL = R.id.mode_url;
    public static final int KEYBOARDMODE_EMAIL = R.id.mode_email;
    public static final int KEYBOARDMODE_IM = R.id.mode_im;
    public static final int KEYBOARDMODE_WEB = R.id.mode_webentry;
    // Main keyboard layouts with the settings key
    public static final int KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY =
            R.id.mode_normal_with_settings_key;
    public static final int KEYBOARDMODE_URL_WITH_SETTINGS_KEY =
            R.id.mode_url_with_settings_key;
    public static final int KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY =
            R.id.mode_email_with_settings_key;
    public static final int KEYBOARDMODE_IM_WITH_SETTINGS_KEY =
            R.id.mode_im_with_settings_key;
    public static final int KEYBOARDMODE_WEB_WITH_SETTINGS_KEY =
            R.id.mode_webentry_with_settings_key;

    // Symbols keyboard layout without the settings key
    public static final int KEYBOARDMODE_SYMBOLS = R.id.mode_symbols;
    // Symbols keyboard layout with the settings key
    public static final int KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY =
            R.id.mode_symbols_with_settings_key;

    public static final String DEFAULT_LAYOUT_ID = "5";
    public static final String PREF_KEYBOARD_LAYOUT = "keyboard_layout";
    /*private static final int[] THEMES = new int [] {
        R.layout.input_basic, R.layout.input_stone, R.layout.input_white, 
        R.layout.input_iphone, R.layout.input_gingerbread, R.layout.input_holo, 
        R.layout.input_galaxy };*/

    // Tables which contains resource ids for each character theme color
    private static final int KBD_PHONE = R.xml.kbd_phone;
    private static final int KBD_PHONE_SYMBOLS = R.xml.kbd_phone_symbols;
    private static final int KBD_SYMBOLS = R.xml.kbd_symbols;
    private static final int KBD_SYMBOLS_SHIFT = R.xml.kbd_symbols_shift;
    /* package */ static final int KBD_QWERTY = R.xml.kbd_qwerty;
    private static final int KBD_SYMBOLS_5ROWS = R.xml.kbd_symbols_5rows;
    private static final int KBD_SYMBOLS_SHIFT_5ROWS = R.xml.kbd_symbols_shift_5rows;
    /* package */ static final int KBD_QWERTY_5ROWS = R.xml.kbd_qwerty_5rows;
    private static final int KBD_NUMBER = R.xml.kbd_number;
    private static final int KBD_NUMBER_SYMBOLS = R.xml.kbd_number_symbols;

    private static final int SYMBOLS_MODE_STATE_NONE = 0;
    private static final int SYMBOLS_MODE_STATE_BEGIN = 1;
    private static final int SYMBOLS_MODE_STATE_SYMBOL = 2;
    
    static final int LANGUAGE_SWICH_BOTH = 0;
    static final int LANGUAGE_SWICH_SLIDE = 1;
    static final int LANGUAGE_SWICH_TOGGLE = 2;
    
    // Extended Row Show Modes
    private static final int EXTENDED_ROW_HIDE = 0;
    private static final int EXTENDED_ROW_SHOW_PORTRAIT = 1;
    private static final int EXTENDED_ROW_SHOW = 2;

    private SoftKeyboardView mInputView;
    private static final int[] ALPHABET_MODES = {
        KEYBOARDMODE_NORMAL,
        KEYBOARDMODE_URL,
        KEYBOARDMODE_EMAIL,
        KEYBOARDMODE_IM,
        KEYBOARDMODE_WEB,
        KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY,
        KEYBOARDMODE_URL_WITH_SETTINGS_KEY,
        KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY,
        KEYBOARDMODE_IM_WITH_SETTINGS_KEY,
        KEYBOARDMODE_WEB_WITH_SETTINGS_KEY };

    private final LatinIME mInputMethodService;
    
    private int mThemeResId;
    private Context mThemedContext;

    private KeyboardId mSymbolsId;
    private KeyboardId mSymbolsShiftedId;

    private KeyboardId mCurrentId;
    private final HashMap<KeyboardId, SoftReference<SoftKeyboard>> mKeyboards;

    private int mMode = MODE_NONE; /** One of the MODE_XXX values */
    private int mImeOptions;
    private boolean mIsSymbols;
    /** mIsAutoCompletionActive indicates that auto completed word will be input instead of
     * what user actually typed. */
    private boolean mIsAutoCompletionActive;
    private boolean mPreferSymbols;
    private int mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;

    // Indicates whether or not we have the settings key
    private boolean mHasSettingsKey;
    /*private static final int SETTINGS_KEY_MODE_AUTO = R.string.settings_key_mode_auto;
    private static final int SETTINGS_KEY_MODE_ALWAYS_SHOW = R.string.settings_key_mode_always_show;
    // NOTE: No need to have SETTINGS_KEY_MODE_ALWAYS_HIDE here because it's not being referred to
    // in the source code now.
    // Default is SETTINGS_KEY_MODE_AUTO.
    private static final int DEFAULT_SETTINGS_KEY_MODE = SETTINGS_KEY_MODE_AUTO;*/
    
    // Indicates whether or not we have the language key
    private int mLanguageSwitchMode;
    
    private boolean mAutoHideMiniKeyboard; // SMM
    private int mKeyboardBackgroundColor; // SMM

    private int mLastDisplayWidth;
    private LanguageSwitcher mLanguageSwitcher;
    private Locale mInputLocale;

    private int mLayoutId;
    private int mShowExtendedRow;

    public KeyboardSwitcher(LatinIME ims) {
        mInputMethodService = ims;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ims);
        mLayoutId = Integer.valueOf(prefs.getString(PREF_KEYBOARD_LAYOUT, DEFAULT_LAYOUT_ID));
        updateSettingsKeyState(prefs);
        prefs.registerOnSharedPreferenceChangeListener(this);

        updateAutoHideMiniKeyboardState(PreferenceManager.getDefaultSharedPreferences(mInputMethodService)); // SMM
        updateKeyboardBackgroundColor(PreferenceManager.getDefaultSharedPreferences(mInputMethodService)); // SMM
        updateExtendedRowState(PreferenceManager.getDefaultSharedPreferences(mInputMethodService)); // SMM
        
        mKeyboards = new HashMap<KeyboardId, SoftReference<SoftKeyboard>>();
        mSymbolsId = makeSymbolsId(getKeyboardSymbolRowsResId());
        mSymbolsShiftedId = makeSymbolsId(getKeyboardSymbolShiftRowsResId());
    }

    /**
     * Sets the input locale, when there are multiple locales for input.
     * If no locale switching is required, then the locale should be set to null.
     * @param locale the current input locale, or null for default locale with no locale 
     * button.
     */
    public void setLanguageSwitcher(LanguageSwitcher languageSwitcher) {
        mLanguageSwitcher = languageSwitcher;
        mInputLocale = mLanguageSwitcher.getInputLocale();
    }

    private KeyboardId makeSymbolsId(int xml) {
        return new KeyboardId(xml, mHasSettingsKey || getHasLanguageKey() ?
                KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY : KEYBOARDMODE_SYMBOLS,
                false);
    }

    public void makeKeyboards(boolean forceCreate) {
        mSymbolsId = makeSymbolsId(getKeyboardSymbolRowsResId());
        mSymbolsShiftedId = makeSymbolsId(getKeyboardSymbolShiftRowsResId());

        if (forceCreate) mKeyboards.clear();
        // Configuration change is coming after the keyboard gets recreated. So don't rely on that.
        // If keyboards have already been made, check if we have a screen width change and 
        // create the keyboard layouts again at the correct orientation
        int displayWidth = mInputMethodService.getMaxWidth();
        if (displayWidth == mLastDisplayWidth) return;
        mLastDisplayWidth = displayWidth;
        if (!forceCreate) mKeyboards.clear();
    }

    /**
     * Represents the parameters necessary to construct a new SoftKeyboard,
     * which also serve as a unique identifier for each keyboard type.
     */
    private static class KeyboardId {
        // TODO: should have locale and portrait/landscape orientation?
        public final int mXml;
        public final int mKeyboardMode; /** A KEYBOARDMODE_XXX value */
        public final boolean mEnableShiftLock;

        private final int mHashCode;

        public KeyboardId(int xml, int mode, boolean enableShiftLock) {
            this.mXml = xml;
            this.mKeyboardMode = mode;
            this.mEnableShiftLock = enableShiftLock;

            this.mHashCode = Arrays.hashCode(new Object[] {
               xml, mode, enableShiftLock
            });
        }

        public KeyboardId(int xml) {
            this(xml, 0, false);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof KeyboardId && equals((KeyboardId) other);
        }

        private boolean equals(KeyboardId other) {
            return other.mXml == this.mXml
                && other.mKeyboardMode == this.mKeyboardMode
                && other.mEnableShiftLock == this.mEnableShiftLock;
        }

        @Override
        public int hashCode() {
            return mHashCode;
        }
    }

    public void setKeyboardMode(int mode, int imeOptions) {
        mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;
        mPreferSymbols = mode == MODE_SYMBOLS;
        if (mode == MODE_SYMBOLS) {
            mode = MODE_TEXT;
        }
        try {
            setKeyboardMode(mode, imeOptions, mPreferSymbols);
        } catch (RuntimeException e) {
        	IMELogger.logOnException(mode + "," + imeOptions + "," + mPreferSymbols, e);
        }
    }

    private void setKeyboardMode(int mode, int imeOptions, boolean isSymbols) {
        if (mInputView == null) return;
        mMode = mode;
        mImeOptions = imeOptions;
        mIsSymbols = isSymbols;

        mInputView.setTextSizeScale(mInputMethodService.getKeyTextSizeScale());
        mInputView.setPreviewEnabled(mInputMethodService.getPopupOn());
        KeyboardId id = getKeyboardId(mode, imeOptions, isSymbols);
        SoftKeyboard keyboard = null;
        keyboard = getKeyboard(id);

        mCurrentId = id;
        mInputView.setKeyboard(keyboard);
        keyboard.setShifted(false);
        keyboard.setShiftLocked(keyboard.isShiftLocked());
        keyboard.setImeOptions(mInputMethodService.getResources(), mMode, imeOptions);
        keyboard.setColorOfSymbolIcons(mIsAutoCompletionActive, 
        		mInputView.getLanguagebarTextColor(), mInputView.getLanguagebarShadowColor());
        // Update the settings key state because number of enabled IMEs could have been changed
        updateSettingsKeyState(PreferenceManager.getDefaultSharedPreferences(mInputMethodService));
        updateLanguageKeyState(PreferenceManager.getDefaultSharedPreferences(mInputMethodService));
    }

    private SoftKeyboard getKeyboard(KeyboardId id) {
        SoftReference<SoftKeyboard> ref = mKeyboards.get(id);
        SoftKeyboard keyboard = (ref == null) ? null : ref.get();
        if (keyboard == null) {
            Resources orig = mInputMethodService.getResources();
            Configuration conf = orig.getConfiguration();
            Locale saveLocale = conf.locale;
            conf.locale = mInputLocale;
            orig.updateConfiguration(conf, null);
            if (mThemedContext != null) {
            	keyboard = new SoftKeyboard(mThemedContext, id.mXml, id.mKeyboardMode, mThemeResId);
            } else {
            	keyboard = new SoftKeyboard(mInputMethodService, id.mXml, id.mKeyboardMode);
            }
            keyboard.setLanguageSwitcher(mLanguageSwitcher, mIsAutoCompletionActive, 
            		mInputView.getLanguagebarTextColor(), mInputView.getLanguagebarShadowColor(),
            		mLanguageSwitchMode);

            if (id.mEnableShiftLock) {
                keyboard.enableShiftLock();
            }
            mKeyboards.put(id, new SoftReference<SoftKeyboard>(keyboard));

            conf.locale = saveLocale;
            orig.updateConfiguration(conf, null);
        }
        return keyboard;
    }

    private KeyboardId getKeyboardId(int mode, int imeOptions, boolean isSymbols) {
    	int keyboardSymbolRowsResId = getKeyboardSymbolRowsResId();
        int keyboardRowsResId = getKeyboardRowsResId();
        if (isSymbols) {
            if (mode == MODE_PHONE) {
                return new KeyboardId(KBD_PHONE_SYMBOLS);
            } else if (mode == MODE_NUMBER) {
            	return new KeyboardId(KBD_NUMBER_SYMBOLS);
            } else {
                return new KeyboardId(keyboardSymbolRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY : KEYBOARDMODE_SYMBOLS,
                        false);
            }
        }
        switch (mode) {
            case MODE_NONE:
            	IMELogger.logOnWarning(
                        "getKeyboardId:" + mode + "," + imeOptions + "," + isSymbols);
                /* fall through */
            case MODE_TEXT:
                return new KeyboardId(keyboardRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY : KEYBOARDMODE_NORMAL,
                        true);
            case MODE_SYMBOLS:
                return new KeyboardId(keyboardSymbolRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY : KEYBOARDMODE_SYMBOLS,
                        false);
            case MODE_PHONE:
                return new KeyboardId(KBD_PHONE);
            case MODE_URL:
                return new KeyboardId(keyboardRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_URL_WITH_SETTINGS_KEY : KEYBOARDMODE_URL, true);
            case MODE_EMAIL:
                return new KeyboardId(keyboardRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY : KEYBOARDMODE_EMAIL, true);
            case MODE_IM:
                return new KeyboardId(keyboardRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_IM_WITH_SETTINGS_KEY : KEYBOARDMODE_IM, true);
            case MODE_WEB:
                return new KeyboardId(keyboardRowsResId, mHasSettingsKey || getHasLanguageKey() ?
                        KEYBOARDMODE_WEB_WITH_SETTINGS_KEY : KEYBOARDMODE_WEB, true);
            case MODE_NUMBER:
            	return new KeyboardId(KBD_NUMBER);
        }
        return null;
    }
    
    private int getKeyboardRowsResId() {
    	return isShowExtendedRow() ? KBD_QWERTY_5ROWS : KBD_QWERTY;
    }
    
    private int getKeyboardSymbolRowsResId() {
    	return isShowExtendedRow() ? KBD_SYMBOLS_5ROWS : KBD_SYMBOLS;
    }
    
    private int getKeyboardSymbolShiftRowsResId() {
    	return isShowExtendedRow() ? KBD_SYMBOLS_SHIFT_5ROWS : KBD_SYMBOLS_SHIFT;
    }

    public int getKeyboardMode() {
        return mMode;
    }
    
    public int getImeOptions() {
    	return mImeOptions;
    }
    
    public boolean getHasLanguageKey() {
    	return ((mLanguageSwitchMode == LANGUAGE_SWICH_TOGGLE) || (mLanguageSwitchMode == LANGUAGE_SWICH_BOTH))  
    			&& (mLanguageSwitcher != null) && mLanguageSwitcher.getLanguageSwitchEnabled();
    }
    
    public boolean isAlphabetMode() {
        if (mCurrentId == null) {
            return false;
        }
        int currentMode = mCurrentId.mKeyboardMode;
        for (Integer mode : ALPHABET_MODES) {
            if (currentMode == mode) {
                return true;
            }
        }
        return false;
    }

    public void setShifted(boolean shifted) {
        if (mInputView != null) {
            mInputView.setShifted(shifted);
        }
    }

    public void setShiftLocked(boolean shiftLocked) {
        if (mInputView != null) {
            mInputView.setShiftLocked(shiftLocked);
        }
    }

    public void toggleShift() {
        if (isAlphabetMode())
            return;
        if (mCurrentId.equals(mSymbolsId) || !mCurrentId.equals(mSymbolsShiftedId)) {
            SoftKeyboard symbolsShiftedKeyboard = getKeyboard(mSymbolsShiftedId);
            mCurrentId = mSymbolsShiftedId;
            mInputView.setKeyboard(symbolsShiftedKeyboard);
            // Symbol shifted keyboard has an ALT key that has a caps lock style indicator. To
            // enable the indicator, we need to call enableShiftLock() and setShiftLocked(true).
            // Thus we can keep the ALT key's Key.on value true while LatinKey.onRelease() is
            // called.
            symbolsShiftedKeyboard.enableShiftLock();
            symbolsShiftedKeyboard.setShiftLocked(true);
            symbolsShiftedKeyboard.setImeOptions(mInputMethodService.getResources(), mMode, mImeOptions);
        } else {
            SoftKeyboard symbolsKeyboard = getKeyboard(mSymbolsId);
            mCurrentId = mSymbolsId;
            mInputView.setKeyboard(symbolsKeyboard);
            // Symbol keyboard has an ALT key that has a caps lock style indicator. To disable the
            // indicator, we need to call enableShiftLock() and setShiftLocked(false).
            symbolsKeyboard.enableShiftLock();
            symbolsKeyboard.setShifted(false);
            symbolsKeyboard.setImeOptions(mInputMethodService.getResources(), mMode, mImeOptions);
        }
    }

    public void toggleSymbols() {
        setKeyboardMode(mMode, mImeOptions, !mIsSymbols);
        if (mIsSymbols && !mPreferSymbols) {
            mSymbolsModeState = SYMBOLS_MODE_STATE_BEGIN;
        } else {
            mSymbolsModeState = SYMBOLS_MODE_STATE_NONE;
        }
    }

    public boolean hasDistinctMultitouch() {
        return mInputView != null && mInputView.hasDistinctMultitouch();
    }

    /**
     * Updates state machine to figure out when to automatically switch back to alpha mode.
     * Returns true if the keyboard needs to switch back 
     */
    public boolean onKey(int key) {
        // Switch back to alpha mode if user types one or more non-space/enter characters
        // followed by a space/enter
        switch (mSymbolsModeState) {
            case SYMBOLS_MODE_STATE_BEGIN:
                if (key != KeyCodes.KEYCODE_SPACE && key != KeyCodes.KEYCODE_ENTER && key > 0) {
                    mSymbolsModeState = SYMBOLS_MODE_STATE_SYMBOL;
                }
                break;
            case SYMBOLS_MODE_STATE_SYMBOL:
                if (key == KeyCodes.KEYCODE_ENTER || key == KeyCodes.KEYCODE_SPACE) return true;
                break;
        }
        return false;
    }

    public SoftKeyboardView getInputView() {
        return mInputView;
    }

    public void recreateInputView() {
        changeSoftKeyboardView(mLayoutId, true);
    }

    @SuppressLint("InflateParams")
	private void changeSoftKeyboardView(int newLayout, boolean forceReset) {
        if (mLayoutId != newLayout || mInputView == null || forceReset) {
            if (mInputView != null) {
                mInputView.closing();
            }
            /*if (THEMES.length <= newLayout) {
                newLayout = Integer.valueOf(DEFAULT_LAYOUT_ID);
            }*/
            
            mThemeResId = KeyboardTheme.getThemeResId(newLayout);
            mThemedContext = new ContextThemeWrapper(mInputMethodService, mThemeResId);

            IMEUtil.GCUtils.getInstance().reset();
            boolean tryGC = true;
            for (int i = 0; i < IMEUtil.GCUtils.GC_TRY_LOOP_MAX && tryGC; ++i) {
                try {
                    //mInputView = (SoftKeyboardView) mInputMethodService.getLayoutInflater().inflate(THEMES[newLayout], null);
                	mInputView = (SoftKeyboardView)LayoutInflater.from(mThemedContext).inflate(R.layout.input_view, null);
                    tryGC = false;
                } catch (OutOfMemoryError e) {
                    tryGC = IMEUtil.GCUtils.getInstance().tryGCOrWait(mLayoutId + "," + newLayout, e);
                } catch (InflateException e) {
                    tryGC = IMEUtil.GCUtils.getInstance().tryGCOrWait(mLayoutId + "," + newLayout, e);
                }
            }
            
            mInputView.setStyle(mThemedContext, mThemeResId, mKeyboardBackgroundColor);
            mInputView.setAutoHideMiniKeyboard(mAutoHideMiniKeyboard);
            mInputView.setOnKeyboardActionListener(mInputMethodService);
            mLayoutId = newLayout;
        }
        mInputMethodService.mHandler.post(new Runnable() {
            public void run() {
                if (mInputView != null) {
                    mInputMethodService.setInputView(mInputView);
                }
                mInputMethodService.updateInputViewShown();
            }});
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_KEYBOARD_LAYOUT.equals(key)) {
            changeSoftKeyboardView(Integer.valueOf(sharedPreferences.getString(key, DEFAULT_LAYOUT_ID)), false);
        } else if (IMESettings.PREF_SETTINGS_KEY.equals(key)) {
            updateSettingsKeyState(sharedPreferences);
            recreateInputView();
        } else if (IMESettings.PREF_LANGUAGE_KEY.equals(key)) {
        	updateLanguageKeyState(sharedPreferences);
            recreateInputView();
        } else if (IMESettings.PREF_AUTO_HIDE_MINIKEYBOARD.equals(key)) {
        	updateAutoHideMiniKeyboardState(sharedPreferences);
        	recreateInputView();
        } else if (IMESettings.PREF_EXTENDED_ROW.equals(key)) {
        	updateExtendedRowState(sharedPreferences);
        	recreateInputView();
        } else if (IMESettings.PREF_KEYBOARD_BACKGROUND_COLOR.equals(key)) {
        	updateKeyboardBackgroundColor(sharedPreferences);
        	recreateInputView();
        }
    }

    public void onAutoCompletionStateChanged(boolean isAutoCompletion) {
        if (isAutoCompletion != mIsAutoCompletionActive) {
            SoftKeyboardView keyboardView = getInputView();
            mIsAutoCompletionActive = isAutoCompletion;
            keyboardView.invalidateKey(((SoftKeyboard) keyboardView.getKeyboard())
                    .onAutoCompletionStateChanged(isAutoCompletion));
        }
    }

    // SMM {
    private void updateSettingsKeyState(SharedPreferences prefs) {
        Resources resources = mInputMethodService.getResources();
        mHasSettingsKey = prefs.getBoolean(IMESettings.PREF_SETTINGS_KEY, resources.getBoolean(R.bool.default_show_settings_key));
    }
    
    private void updateLanguageKeyState(SharedPreferences prefs) {
        Resources resources = mInputMethodService.getResources();
        mLanguageSwitchMode = Integer.valueOf(prefs.getString(IMESettings.PREF_LANGUAGE_KEY, resources.getString(R.string.default_language_key_mode)));
    }
    
    private void updateAutoHideMiniKeyboardState(SharedPreferences prefs) {
        Resources resources = mInputMethodService.getResources();
        mAutoHideMiniKeyboard = prefs.getBoolean(IMESettings.PREF_AUTO_HIDE_MINIKEYBOARD, resources.getBoolean(R.bool.default_auto_hide_minikeyboard));
    }
    
    private void updateExtendedRowState(SharedPreferences prefs) {
        Resources resources = mInputMethodService.getResources();
        mShowExtendedRow = Integer.valueOf(prefs.getString(IMESettings.PREF_EXTENDED_ROW, resources.getString(R.string.enabled_extened_row_default_value)));
    }
    
    private void updateKeyboardBackgroundColor(SharedPreferences prefs) {
        Resources resources = mInputMethodService.getResources();
        mKeyboardBackgroundColor = prefs.getInt(IMESettings.PREF_KEYBOARD_BACKGROUND_COLOR, resources.getColor(R.color.transparent));
    }
    
    public boolean isShowExtendedRow() {
    	if (mInputMethodService == null) {
    		return false;
    	}
    	int orientation = mInputMethodService.getOrientation();
    	switch(mShowExtendedRow) {
    	case EXTENDED_ROW_SHOW_PORTRAIT:
    		return (orientation != Configuration.ORIENTATION_LANDSCAPE);
    	case EXTENDED_ROW_HIDE:
    		return false;
    	case EXTENDED_ROW_SHOW:
    	default:
    		return true;
    	}
    }
    
    public LanguageSwitcher getLanguageSwitcher() {
    	return mLanguageSwitcher;
    }
    
    public boolean isAutoCompletionActive() {
    	return mIsAutoCompletionActive;
    }
    
    public int getThemeResId() {
    	return mThemeResId;
    }
    
    public Context getThemedContext() {
    	return mThemedContext;
    }
    // } SMM
}
