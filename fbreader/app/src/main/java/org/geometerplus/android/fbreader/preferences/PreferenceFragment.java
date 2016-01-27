/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.preferences;

import java.text.DecimalFormatSymbols;
import java.util.*;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.LinearLayout;

import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.language.Language;
import org.geometerplus.zlibrary.core.language.LanguageUtil;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.JsonRequest;
import org.geometerplus.zlibrary.core.options.*;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.text.view.style.*;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;

import org.fbreader.common.options.*;
import org.fbreader.reader.ActionCode;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.*;
import org.geometerplus.fbreader.network.sync.SyncData;
import org.geometerplus.fbreader.network.sync.SyncUtil;
import org.geometerplus.fbreader.tips.TipsManager;

import org.fbreader.common.android.FBReaderUtil;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.preferences.background.BackgroundPreference;
import org.geometerplus.android.fbreader.preferences.menu.MenuPreference;
import org.geometerplus.android.fbreader.sync.SyncOperations;

import org.geometerplus.android.util.DeviceType;
import org.geometerplus.android.util.UIUtil;

public class PreferenceFragment extends android.preference.PreferenceFragment {
	private PreferenceScreen myScreen;
	private final HashMap<String,Screen> myScreenMap = new HashMap<String,Screen>();

	private static class Screen {
		private final PreferenceManager myManager;
		private final PreferenceActivity myActivity;
		public final ZLResource Resource;
		private final PreferenceScreen myScreen;

		private Screen(PreferenceManager manager, PreferenceActivity activity, ZLResource root, String resourceKey) {
			myManager = manager;
			myActivity = activity;
			Resource = root.getResource(resourceKey);
			myScreen = manager.createPreferenceScreen(activity);
			myScreen.setTitle(Resource.getValue());
			myScreen.setSummary(Resource.getResource("summary").getValue());
		}

		public void setSummary(CharSequence summary) {
			myScreen.setSummary(summary);
		}

		public Screen createPreferenceScreen(String resourceKey) {
			Screen screen = new Screen(myManager, myActivity, Resource, resourceKey);
			myScreen.addPreference(screen.myScreen);
			return screen;
		}

		public Preference addPreference(Preference preference) {
			myScreen.addPreference(preference);
			return preference;
		}

		public Preference addOption(ZLBooleanOption option, String resourceKey) {
			return addPreference(
				new ZLBooleanPreference(myActivity, option, Resource.getResource(resourceKey))
			);
		}

		public Preference addOption(ZLColorOption option, String resourceKey) {
			return addPreference(
				new ZLColorPreference(myActivity, Resource, resourceKey, option)
			);
		}

		public Preference addOption(ZLIntegerRangeOption option, String resourceKey) {
			return addPreference(new ZLIntegerRangePreference(
				myActivity, Resource.getResource(resourceKey), option
			));
		}

		public <T extends Enum<T>> Preference addOption(ZLEnumOption<T> option, String key) {
			return addPreference(
				new ZLEnumPreference<T>(myActivity, option, Resource.getResource(key))
			);
		}

		public <T extends Enum<T>> Preference addOption(ZLEnumOption<T> option, String key, String valuesKey) {
			return addPreference(
				new ZLEnumPreference<T>(myActivity, option, Resource.getResource(key), Resource.getResource(valuesKey))
			);
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen oldScreen, Preference preference) {
		final boolean result = super.onPreferenceTreeClick(oldScreen, preference);

		if (preference instanceof PreferenceScreen) {
			final Dialog dialog = ((PreferenceScreen)preference).getDialog();
			View view = dialog.findViewById(android.R.id.list);
			while (view != null && !(view instanceof LinearLayout)) {
				final ViewParent parent = view.getParent();
				view = parent instanceof View ? (View)parent : null;
			}
			if (view instanceof LinearLayout) {
				final PreferenceActivity activity = (PreferenceActivity)getActivity();
				final LinearLayout ll = (LinearLayout)view;
				final Toolbar toolbar =
					(Toolbar)activity.getLayoutInflater().inflate(R.layout.md_toolbar, ll, false);
				toolbar.setNavigationOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dialog.dismiss();
					}
				});
				activity.setupToolbarAppearance(toolbar, true);
				toolbar.setTitle(preference.getTitle());
				ll.addView(toolbar, 0);
			}
		}

		return result;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		final PreferenceActivity activity = (PreferenceActivity)getActivity();
		if (activity == null) {
			return;
		}

		myScreen = getPreferenceManager().createPreferenceScreen(activity);

		final Intent intent = activity.getIntent();
		final Uri data = intent.getData();
		final String screenId;
		if (Intent.ACTION_VIEW.equals(intent.getAction())
				&& data != null && "fbreader-preferences".equals(data.getScheme())) {
			screenId = data.getEncodedSchemeSpecificPart();
		} else {
			screenId = intent.getStringExtra(PreferenceActivity.SCREEN_KEY);
		}

		Config.Instance().runOnConnect(new Runnable() {
			public void run() {
				init(activity, intent);
				final Screen screen = myScreenMap.get(screenId);
				setPreferenceScreen(screen != null ? screen.myScreen : myScreen);
				if (screen != null) {
					activity.setTitle(screen.Resource.getValue());
				}
			}
		});
	}

	private void init(final PreferenceActivity activity, Intent intent) {
		final Config config = Config.Instance();
		config.requestAllValuesForGroup("Style");
		config.requestAllValuesForGroup("Options");
		config.requestAllValuesForGroup("LookNFeel");
		config.requestAllValuesForGroup("Fonts");
		config.requestAllValuesForGroup("Files");
		config.requestAllValuesForGroup("Scrolling");
		config.requestAllValuesForGroup("Colors");
		config.requestAllValuesForGroup("Sync");
		config.requestAllValuesForGroup("ReadingModeMenu");
		activity.setResult(FBReader.RESULT_REPAINT);

		final ViewOptions viewOptions = new ViewOptions();
		final MiscOptions miscOptions = new MiscOptions();
		final FooterOptions footerOptions = viewOptions.getFooterOptions();
		final PageTurningOptions pageTurningOptions = new PageTurningOptions();
		final ImageOptions imageOptions = new ImageOptions();
		final SyncOptions syncOptions = new SyncOptions();
		final ColorProfile profile = viewOptions.getColorProfile();
		final ZLTextStyleCollection collection = viewOptions.getTextStyleCollection();
		final ZLKeyBindings keyBindings = new ZLKeyBindings();

		final ZLAndroidLibrary androidLibrary = FBReaderUtil.getZLibrary(activity);
		final Locale uiLocale =
			Language.uiLocale(FBReaderUtil.activityLocale(activity));
		final String decimalSeparator = String.valueOf(
			new DecimalFormatSymbols(uiLocale).getDecimalSeparator()
		);

		final Screen directoriesScreen = createPreferenceScreen(activity, "directories");
		final Runnable libraryUpdater = new Runnable() {
			public void run() {
				final BookCollectionShadow bookCollection = new BookCollectionShadow();
				bookCollection.bindToService(activity, new Runnable() {
					public void run() {
						bookCollection.reset(false);
						bookCollection.unbind();
					}
				});
			}
		};
		directoriesScreen.addPreference(activity.ChooserCollection.createPreference(
			directoriesScreen.Resource, "bookPath", Paths.BookPathOption, libraryUpdater
		));
		directoriesScreen.addPreference(activity.ChooserCollection.createPreference(
			directoriesScreen.Resource, "downloadDir", Paths.DownloadsDirectoryOption, libraryUpdater
		));
		final PreferenceSet fontReloader = new PreferenceSet.Reloader();
		directoriesScreen.addPreference(activity.ChooserCollection.createPreference(
			directoriesScreen.Resource, "fontPath", Paths.FontPathOption, fontReloader
		));
		directoriesScreen.addPreference(activity.ChooserCollection.createPreference(
			directoriesScreen.Resource, "tempDir", Paths.TempDirectoryOption(activity), null
		));

		final Screen syncScreen = createPreferenceScreen(activity, "sync");
		final PreferenceSet syncPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				return syncOptions.Enabled.getValue();
			}
		};
		syncScreen.addPreference(new UrlPreference(activity, syncScreen.Resource, "site"));
		syncScreen.addPreference(new ZLCheckBoxPreference(
			activity, syncScreen.Resource.getResource("enable")
		) {
			{
				if (syncOptions.Enabled.getValue()) {
					setChecked(true);
					setOnSummary(SyncUtil.getAccountName(activity.NetworkContext));
				} else {
					setChecked(false);
				}
			}

			private void enableSynchronisation() {
				SyncOperations.enableSync(activity, syncOptions);
			}

			@Override
			protected void onClick() {
				super.onClick();
				syncPreferences.run();

				if (!isChecked()) {
					SyncUtil.logout(activity.NetworkContext);
					syncOptions.Enabled.setValue(false);
					enableSynchronisation();
					syncPreferences.run();
					new SyncData().reset();
					return;
				}

				UIUtil.createExecutor(activity, "tryConnect").execute(new Runnable() {
					public void run() {
						try {
							activity.NetworkContext.perform(
								new JsonRequest(SyncOptions.BASE_URL + "login/test") {
									@Override
									public void processResponse(Object response) {
										final String account = (String)((Map)response).get("user");
										syncOptions.Enabled.setValue(account != null);
										enableSynchronisation();
										activity.runOnUiThread(new Runnable() {
											public void run() {
												setOnSummary(account);
												syncPreferences.run();
											}
										});
									}
								}
							);
						} catch (ZLNetworkException e) {
							e.printStackTrace();
							activity.runOnUiThread(new Runnable() {
								public void run() {
									setChecked(false);
								}
							});
						}
					}
				}, null);
			}

			private void setOnSummary(String account) {
				final String summary = account != null
					? Resource.getResource("summaryOnWithAccount").getValue().replace("%s", account)
					: Resource.getResource("summaryOn").getValue();
				activity.runOnUiThread(new Runnable() {
					public void run() {
						setSummaryOn(summary);
					}
				});
			}
		});
		syncPreferences.add(syncScreen.addOption(syncOptions.UploadAllBooks, "uploadAllBooks", "values"));
		syncPreferences.add(syncScreen.addOption(syncOptions.Positions, "positions", "values"));
		syncPreferences.add(syncScreen.addOption(syncOptions.ChangeCurrentBook, "changeCurrentBook"));
		//syncPreferences.add(syncScreen.addOption(syncOptions.Metainfo, "metainfo", "values"));
		syncPreferences.add(syncScreen.addOption(syncOptions.Bookmarks, "bookmarks", "values"));
		syncPreferences.add(syncScreen.addOption(syncOptions.CustomShelves, "customShelves", "values"));
		syncPreferences.run();

		final Screen appearanceScreen = createPreferenceScreen(activity, "appearance");
		appearanceScreen.addPreference(new LanguagePreference(
			activity, appearanceScreen.Resource.getResource("language"), ZLResource.interfaceLanguages()
		) {
			@Override
			protected String currentValue() {
				return Language.uiLanguageOption().getValue();
			}

			@Override
			protected void onValueSelected(int index, String code) {
				final ZLStringOption languageOption = Language.uiLanguageOption();
				if (!code.equals(languageOption.getValue())) {
					languageOption.setValue(code);
					activity.finish();
					startActivity(new Intent(
						Intent.ACTION_VIEW, Uri.parse("fbreader-preferences:appearance"),
						activity, PreferenceActivity.class
					));
				}
			}
		});
		appearanceScreen.addPreference(new SingleChoiceStringPreference(
			activity, appearanceScreen.Resource.getResource("screenOrientation"),
			androidLibrary.getOrientationOption(), androidLibrary.allOrientations()
		));
		appearanceScreen.addPreference(new ZLBooleanPreference(
			activity,
			viewOptions.TwoColumnView,
			appearanceScreen.Resource.getResource("twoColumnView")
		));
		appearanceScreen.addPreference(new ZLBooleanPreference(
			activity,
			miscOptions.AllowScreenBrightnessAdjustment,
			appearanceScreen.Resource.getResource("allowScreenBrightnessAdjustment")
		) {
			private final int myLevel = androidLibrary.ScreenBrightnessLevelOption.getValue();

			@Override
			protected void onClick() {
				super.onClick();
				androidLibrary.ScreenBrightnessLevelOption.setValue(isChecked() ? myLevel : 0);
			}
		});
		appearanceScreen.addPreference(new BatteryLevelToTurnScreenOffPreference(
			activity,
			androidLibrary.BatteryLevelToTurnScreenOffOption,
			appearanceScreen.Resource.getResource("dontTurnScreenOff")
		));
		/*
		appearanceScreen.addPreference(new ZLBooleanPreference(
			activity,
			androidLibrary.DontTurnScreenOffDuringChargingOption,
			appearanceScreen.Resource.getResource("dontTurnScreenOffDuringCharging")
		));
		*/
		if (Build.VERSION.SDK_INT >= 19/*Build.VERSION_CODES.KITKAT*/) {
			final PreferenceSet nonFullscreenPrefences = new PreferenceSet.Enabler() {
				@Override
				protected Boolean detectState() {
					return !androidLibrary.EnableFullscreenModeOption.getValue();
				}
			};
			appearanceScreen.addPreference(new ZLBooleanPreference(
				activity, androidLibrary.EnableFullscreenModeOption,
				appearanceScreen.Resource.getResource("fullscreenMode")
			) {
				@Override
				protected void onClick() {
					super.onClick();
					nonFullscreenPrefences.run();
				}
			});
			nonFullscreenPrefences.add(
				appearanceScreen.addOption(androidLibrary.ShowStatusBarOption, "showStatusBar")
			);
			nonFullscreenPrefences.run();
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			appearanceScreen.addOption(androidLibrary.ShowStatusBarOption, "showStatusBar");
		}
		appearanceScreen.addOption(androidLibrary.ShowActionBarOption, "showActionBar");
		appearanceScreen.addOption(androidLibrary.DisableButtonLightsOption, "disableButtonLights");
		appearanceScreen.addOption(miscOptions.EnableBookMenuSwipeGesture, "enableBookMenuSwipeGesture");

		if (DeviceType.Instance().isEInk()) {
			final EInkOptions einkOptions = new EInkOptions();
			final Screen einkScreen = createPreferenceScreen(activity, "eink");
			final PreferenceSet einkPreferences = new PreferenceSet.Enabler() {
				@Override
				protected Boolean detectState() {
					return einkOptions.EnableFastRefresh.getValue();
				}
			};

			einkScreen.addPreference(new ZLBooleanPreference(
				activity, einkOptions.EnableFastRefresh,
				einkScreen.Resource.getResource("enableFastRefresh")
			) {
				@Override
				protected void onClick() {
					super.onClick();
					einkPreferences.run();
				}
			});

			final ZLIntegerRangePreference updateIntervalPreference = new ZLIntegerRangePreference(
				activity, einkScreen.Resource.getResource("interval"), einkOptions.UpdateInterval
			);
			einkScreen.addPreference(updateIntervalPreference);

			einkPreferences.add(updateIntervalPreference);
			einkPreferences.run();
		}

		final Screen textScreen = createPreferenceScreen(activity, "text");

		final Screen fontPropertiesScreen = textScreen.createPreferenceScreen("fontProperties");
		fontPropertiesScreen.addOption(ZLAndroidPaintContext.AntiAliasOption, "antiAlias");
		fontPropertiesScreen.addOption(ZLAndroidPaintContext.DeviceKerningOption, "deviceKerning");
		fontPropertiesScreen.addOption(ZLAndroidPaintContext.DitheringOption, "dithering");
		if (ZLAndroidPaintContext.usesHintingOption()) {
			fontPropertiesScreen.addOption(ZLAndroidPaintContext.HintingOption, "hinting");
		}
		fontPropertiesScreen.addOption(ZLAndroidPaintContext.SubpixelOption, "subpixel");

		final ZLTextBaseStyle baseStyle = collection.getBaseStyle();

		fontReloader.add(textScreen.addPreference(new FontPreference(
			activity, textScreen.Resource.getResource("font"),
			baseStyle.FontFamilyOption, false
		)));
		textScreen.addPreference(new ZLIntegerRangePreference(
			activity, textScreen.Resource.getResource("fontSize"),
			baseStyle.FontSizeOption
		));
		textScreen.addPreference(new FontStylePreference(
			activity, textScreen.Resource.getResource("fontStyle"),
			baseStyle.BoldOption, baseStyle.ItalicOption
		));
		final ZLIntegerRangeOption spaceOption = baseStyle.LineSpaceOption;
		final String[] spacings = new String[spaceOption.MaxValue - spaceOption.MinValue + 1];
		for (int i = 0; i < spacings.length; ++i) {
			final int val = spaceOption.MinValue + i;
			spacings[i] = (char)(val / 10 + '0') + decimalSeparator + (char)(val % 10 + '0');
		}
		textScreen.addPreference(new SingleChoiceIntegerPreference(
			activity, textScreen.Resource.getResource("lineSpacing"),
			spaceOption, spacings
		));
		final String[] alignments = { "left", "right", "center", "justify" };
		textScreen.addPreference(new SingleChoiceIntegerPreference(
			activity, textScreen.Resource.getResource("alignment"),
			baseStyle.AlignmentOption, alignments
		));
		textScreen.addOption(baseStyle.AutoHyphenationOption, "autoHyphenations");

		final Screen moreStylesScreen = textScreen.createPreferenceScreen("more");
		for (ZLTextNGStyleDescription description : collection.getDescriptionList()) {
			final Screen ngScreen = moreStylesScreen.createPreferenceScreen(description.Name);
			ngScreen.addPreference(new FontPreference(
				activity, textScreen.Resource.getResource("font"),
				description.FontFamilyOption, true
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.FontSizeOption,
				StringPreference.CONSTRAINT_POSITIVE_LENGTH,
				textScreen.Resource, "fontSize"
			));
			ngScreen.addPreference(new SingleChoiceStringPreference(
				activity, textScreen.Resource.getResource("bold"),
				description.FontWeightOption,
				new String[] { "inherit", "normal", "bold" }
			));
			ngScreen.addPreference(new SingleChoiceStringPreference(
				activity, textScreen.Resource.getResource("italic"),
				description.FontStyleOption,
				new String[] { "inherit", "normal", "italic" }
			));
			ngScreen.addPreference(new SingleChoiceStringPreference(
				activity, textScreen.Resource.getResource("textDecoration"),
				description.TextDecorationOption,
				new String[] { "inherit", "none", "underline", "line-through" }
			));
			ngScreen.addPreference(new SingleChoiceStringPreference(
				activity, textScreen.Resource.getResource("allowHyphenations"),
				description.HyphenationOption,
				new String[] { "inherit", "none", "auto" }
			));
			ngScreen.addPreference(new SingleChoiceStringPreference(
				activity, textScreen.Resource.getResource("alignment"),
				description.AlignmentOption,
				new String[] { "inherit", "left", "right", "center", "justify" }
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.LineHeightOption,
				StringPreference.CONSTRAINT_PERCENT,
				textScreen.Resource, "lineSpacing"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.MarginTopOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "spaceBefore"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.MarginBottomOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "spaceAfter"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.MarginLeftOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "leftIndent"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.MarginRightOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "rightIndent"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.TextIndentOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "firstLineIndent"
			));
			ngScreen.addPreference(new StringPreference(
				activity, description.VerticalAlignOption,
				StringPreference.CONSTRAINT_LENGTH,
				textScreen.Resource, "verticalAlignment"
			));
		}

		final Screen toastsScreen = createPreferenceScreen(activity, "toast");
		toastsScreen.addOption(miscOptions.ToastFontSizePercent, "fontSizePercent");
		toastsScreen.addOption(miscOptions.ShowFootnoteToast, "showFootnoteToast");
		toastsScreen.addPreference(new ZLEnumPreference(
			activity,
			miscOptions.FootnoteToastDuration,
			toastsScreen.Resource.getResource("footnoteToastDuration"),
			ZLResource.resource("duration")
		));

		final Screen cssScreen = createPreferenceScreen(activity, "css");
		cssScreen.addOption(baseStyle.UseCSSFontFamilyOption, "fontFamily");
		cssScreen.addOption(baseStyle.UseCSSFontSizeOption, "fontSize");
		cssScreen.addOption(baseStyle.UseCSSTextAlignmentOption, "textAlignment");
		cssScreen.addOption(baseStyle.UseCSSMarginsOption, "margins");

		final Screen colorsScreen = createPreferenceScreen(activity, "colors");

		final PreferenceSet backgroundSet = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				return profile.WallpaperOption.getValue().startsWith("/");
			}
		};
		activity.BackgroundPreference = new BackgroundPreference(
			activity,
			profile,
			colorsScreen.Resource.getResource("background"),
			PreferenceActivity.BACKGROUND_REQUEST_CODE
		) {
			@Override
			public void update(Intent data) {
				super.update(data);
				backgroundSet.run();
			}
		};
		colorsScreen.addPreference(activity.BackgroundPreference);
		backgroundSet.add(colorsScreen.addOption(profile.FillModeOption, "fillMode"));
		backgroundSet.run();

		colorsScreen.addOption(profile.RegularTextOption, "text");
		colorsScreen.addOption(profile.HyperlinkTextOption, "hyperlink");
		colorsScreen.addOption(profile.VisitedHyperlinkTextOption, "hyperlinkVisited");
		colorsScreen.addOption(profile.FooterFillOption, "footerOldStyle");
		colorsScreen.addOption(profile.FooterNGBackgroundOption, "footerBackground");
		colorsScreen.addOption(profile.FooterNGForegroundOption, "footerForeground");
		colorsScreen.addOption(profile.FooterNGForegroundUnreadOption, "footerForegroundUnread");
		colorsScreen.addOption(profile.SelectionBackgroundOption, "selectionBackground");
		colorsScreen.addOption(profile.SelectionForegroundOption, "selectionForeground");
		colorsScreen.addOption(profile.HighlightingForegroundOption, "highlightingForeground");
		colorsScreen.addOption(profile.HighlightingBackgroundOption, "highlightingBackground");

		final Screen marginsScreen = createPreferenceScreen(activity, "margins");
		marginsScreen.addOption(viewOptions.LeftMargin, "left");
		marginsScreen.addOption(viewOptions.RightMargin, "right");
		marginsScreen.addOption(viewOptions.TopMargin, "top");
		marginsScreen.addOption(viewOptions.BottomMargin, "bottom");
		marginsScreen.addOption(viewOptions.SpaceBetweenColumns, "spaceBetweenColumns");

		final Screen statusLineScreen = createPreferenceScreen(activity, "scrollBar");

		final PreferenceSet footerPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				switch (viewOptions.ScrollbarType.getValue()) {
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER:
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER_OLD_STYLE:
						return true;
					default:
						return false;
				}
			}
		};
		final PreferenceSet tocPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				switch (viewOptions.ScrollbarType.getValue()) {
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER:
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER_OLD_STYLE:
						return footerOptions.ShowTOCMarks.getValue();
					default:
						return false;
				}
			}
		};
		final PreferenceSet oldStyleFooterPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				switch (viewOptions.ScrollbarType.getValue()) {
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER_OLD_STYLE:
						return true;
					default:
						return false;
				}
			}
		};
		final PreferenceSet newStyleFooterPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				switch (viewOptions.ScrollbarType.getValue()) {
					case ViewOptions.Scrollbar.SHOW_AS_FOOTER:
						return true;
					default:
						return false;
				}
			}
		};


		final String[] scrollBarTypes = {"hide", "show", "showAsProgress", "showAsFooter", "showAsFooterOldStyle"};
		statusLineScreen.addPreference(new SingleChoiceIntegerPreference(
			activity, statusLineScreen.Resource.getResource("scrollbarType"),
			viewOptions.ScrollbarType, scrollBarTypes
		) {
			@Override
			protected void onValueSelected(int index, String value) {
				super.onValueSelected(index, value);
				footerPreferences.run();
				tocPreferences.run();
				oldStyleFooterPreferences.run();
				newStyleFooterPreferences.run();
			}
		});

		footerPreferences.add(statusLineScreen.addPreference(new ZLIntegerRangePreference(
			activity, statusLineScreen.Resource.getResource("footerHeight"),
			viewOptions.FooterHeight
		)));
		oldStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterFillOption, "footerOldStyleColor"));
		newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGBackgroundOption, "footerBackgroundColor"));
		newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGForegroundOption, "footerForegroundColor"));
		newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGForegroundUnreadOption, "footerForegroundUnreadColor"));
		footerPreferences.add(statusLineScreen.addPreference(new ZLBooleanPreference(
			activity,
			footerOptions.ShowTOCMarks,
			statusLineScreen.Resource.getResource("tocMarks")
		) {
			@Override
			protected void onClick() {
				super.onClick();
				tocPreferences.run();
			}
		}));
		tocPreferences.add(statusLineScreen.addOption(footerOptions.MaxTOCMarks, "tocMarksMaxNumber"));
		footerPreferences.add(statusLineScreen.addOption(footerOptions.ShowProgress, "showProgress"));
		footerPreferences.add(statusLineScreen.addOption(footerOptions.ShowClock, "showClock"));
		footerPreferences.add(statusLineScreen.addOption(footerOptions.ShowBattery, "showBattery"));
		footerPreferences.add(statusLineScreen.addPreference(new FontPreference(
			activity, statusLineScreen.Resource.getResource("font"),
			footerOptions.Font, false
		)));
		footerPreferences.run();
		tocPreferences.run();
		oldStyleFooterPreferences.run();
		newStyleFooterPreferences.run();

		/*
		final Screen colorProfileScreen = createPreferenceScreen(activity, "colorProfile");
		final ZLResource resource = colorProfileScreen.Resource;
		colorProfileScreen.setSummary(ColorProfilePreference.createTitle(resource, fbreader.getColorProfileName()));
		for (String key : ColorProfile.names()) {
			colorProfileScreen.addPreference(new ColorProfilePreference(
				activity, fbreader, colorProfileScreen, key, ColorProfilePreference.createTitle(resource, key)
			));
		}
		 */

		final Screen scrollingScreen = createPreferenceScreen(activity, "scrolling");
		scrollingScreen.addOption(pageTurningOptions.FingerScrolling, "fingerScrolling");
		scrollingScreen.addOption(miscOptions.EnableDoubleTap, "enableDoubleTapDetection");

		final PreferenceSet volumeKeysPreferences = new PreferenceSet.Enabler() {
			@Override
			protected Boolean detectState() {
				return keyBindings.hasBinding(KeyEvent.KEYCODE_VOLUME_UP, false);
			}
		};
		scrollingScreen.addPreference(new ZLCheckBoxPreference(
			activity, scrollingScreen.Resource.getResource("volumeKeys")
		) {
			{
				setChecked(keyBindings.hasBinding(KeyEvent.KEYCODE_VOLUME_UP, false));
			}

			@Override
			protected void onClick() {
				super.onClick();
				if (isChecked()) {
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
				} else {
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, FBReaderApp.NoAction);
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, FBReaderApp.NoAction);
				}
				volumeKeysPreferences.run();
			}
		});
		volumeKeysPreferences.add(scrollingScreen.addPreference(new ZLCheckBoxPreference(
			activity, scrollingScreen.Resource.getResource("invertVolumeKeys")
		) {
			{
				setChecked(ActionCode.VOLUME_KEY_SCROLL_FORWARD.equals(
					keyBindings.getBinding(KeyEvent.KEYCODE_VOLUME_UP, false)
				));
			}

			@Override
			protected void onClick() {
				super.onClick();
				if (isChecked()) {
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
				} else {
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
					keyBindings.bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
				}
			}
		}));
		volumeKeysPreferences.run();

		scrollingScreen.addOption(pageTurningOptions.Animation, "animation");
		scrollingScreen.addPreference(new AnimationSpeedPreference(
			activity,
			scrollingScreen.Resource,
			"animationSpeed",
			pageTurningOptions.AnimationSpeed
		));
		scrollingScreen.addOption(pageTurningOptions.Horizontal, "horizontal");

		final Screen dictionaryScreen = createPreferenceScreen(activity, "dictionary");

		final List<String> langCodes = ZLResource.languageCodes();
		final ArrayList<Language> languages = new ArrayList<Language>(langCodes.size() + 1);
		for (String code : langCodes) {
			languages.add(LanguageUtil.language(code));
		}
		Collections.sort(languages);
		languages.add(0, LanguageUtil.language(
			Language.ANY_CODE, dictionaryScreen.Resource.getResource("targetLanguage")
		));
		final LanguagePreference targetLanguagePreference = new LanguagePreference(
			activity, dictionaryScreen.Resource.getResource("targetLanguage"), languages
		) {
			@Override
			protected String currentValue() {
				return DictionaryUtil.TargetLanguageOption.getValue();
			}

			@Override
			protected void onValueSelected(int index, String code) {
				DictionaryUtil.TargetLanguageOption.setValue(code);
			}
		};

		DictionaryUtil.init(activity, new Runnable() {
			public void run() {
				dictionaryScreen.addPreference(new DictionaryPreference(
					activity,
					dictionaryScreen.Resource.getResource("dictionary"),
					DictionaryUtil.singleWordTranslatorOption(),
					DictionaryUtil.dictionaryInfos(activity, true)
				) {
					@Override
					protected void onValueSelected(int index, String value) {
						super.onValueSelected(index, value);
						targetLanguagePreference.setEnabled(
							DictionaryUtil.getCurrentDictionaryInfo(true).SupportsTargetLanguageSetting
						);
					}
				});
				dictionaryScreen.addPreference(new DictionaryPreference(
					activity,
					dictionaryScreen.Resource.getResource("translator"),
					DictionaryUtil.multiWordTranslatorOption(),
					DictionaryUtil.dictionaryInfos(activity, false)
				));
				dictionaryScreen.addPreference(new ZLBooleanPreference(
					activity,
					miscOptions.NavigateAllWords,
					dictionaryScreen.Resource.getResource("navigateOverAllWords")
				));
				dictionaryScreen.addOption(miscOptions.WordTappingAction, "longTapAction");
				dictionaryScreen.addPreference(targetLanguagePreference);
				targetLanguagePreference.setEnabled(
					DictionaryUtil.getCurrentDictionaryInfo(true).SupportsTargetLanguageSetting
				);
			}
		});

		final Screen imagesScreen = createPreferenceScreen(activity, "images");
		imagesScreen.addOption(imageOptions.TapAction, "longTapAction");
		imagesScreen.addOption(imageOptions.FitToScreen, "fitImagesToScreen");
		imagesScreen.addOption(imageOptions.ImageViewBackground, "backgroundColor");
		imagesScreen.addOption(imageOptions.MatchBackground, "matchBackground");

		final Screen menuScreen = createPreferenceScreen(activity, "menu");
		menuScreen.addPreference(new MenuPreference(
			activity,
			menuScreen.Resource.getResource("items"))
		);
		menuScreen.addOption(miscOptions.CoverAsMenuBackground, "backgroundCover");

		final CancelMenuHelper cancelMenuHelper = new CancelMenuHelper();
		final Screen cancelMenuScreen = createPreferenceScreen(activity, "cancelMenu");
		cancelMenuScreen.addOption(cancelMenuHelper.ShowLibraryItemOption, "library");
		cancelMenuScreen.addOption(cancelMenuHelper.ShowNetworkLibraryItemOption, "networkLibrary");
		cancelMenuScreen.addOption(cancelMenuHelper.ShowPreviousBookItemOption, "previousBook");
		cancelMenuScreen.addOption(cancelMenuHelper.ShowPositionItemsOption, "positions");
		final String[] backKeyActions =
			{ ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU };
		cancelMenuScreen.addPreference(new SingleChoiceStringPreference(
			activity, cancelMenuScreen.Resource.getResource("backKeyAction"),
			keyBindings.getOption(KeyEvent.KEYCODE_BACK, false), backKeyActions
		));
		final String[] backKeyLongPressActions =
			{ ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU, FBReaderApp.NoAction };
		cancelMenuScreen.addPreference(new SingleChoiceStringPreference(
			activity, cancelMenuScreen.Resource.getResource("backKeyLongPressAction"),
			keyBindings.getOption(KeyEvent.KEYCODE_BACK, true), backKeyLongPressActions
		));

		final Screen tipsScreen = createPreferenceScreen(activity, "tips");
		tipsScreen.addOption(TipsManager.ShowTipsOption, "showTips");

		final Screen aboutScreen = createPreferenceScreen(activity, "about");
		aboutScreen.addPreference(new InfoPreference(
			activity,
			aboutScreen.Resource.getResource("version").getValue(),
			androidLibrary.getFullVersionName()
		));
		aboutScreen.addPreference(new UrlPreference(activity, aboutScreen.Resource, "site"));
		aboutScreen.addPreference(new UrlPreference(activity, aboutScreen.Resource, "email"));
		aboutScreen.addPreference(new UrlPreference(activity, aboutScreen.Resource, "googleplus"));
		aboutScreen.addPreference(new UrlPreference(activity, aboutScreen.Resource, "twitter"));
		aboutScreen.addPreference(new UrlPreference(activity, aboutScreen.Resource, "facebook"));
		aboutScreen.addPreference(new ThirdPartyLibrariesPreference(activity, aboutScreen.Resource, "thirdParty"));
	}

	Screen createPreferenceScreen(PreferenceActivity activity, String resourceKey) {
		final Screen screen = new Screen(getPreferenceManager(), activity, PreferenceActivity.Resource, resourceKey);
		myScreenMap.put(resourceKey, screen);
		myScreen.addPreference(screen.myScreen);
		return screen;
	}
}
