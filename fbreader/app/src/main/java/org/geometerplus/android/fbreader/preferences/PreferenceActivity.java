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
import android.preference.*;
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
import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext;

import org.fbreader.common.options.*;
import org.fbreader.reader.ActionCode;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.bookmodel.FBTextKind;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.*;
import org.geometerplus.fbreader.network.sync.SyncData;
import org.geometerplus.fbreader.network.sync.SyncUtil;
import org.geometerplus.fbreader.tips.TipsManager;

import org.fbreader.common.android.FBReaderUtil;

import org.geometerplus.android.fbreader.*;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.fbreader.network.auth.ActivityNetworkContext;
import org.geometerplus.android.fbreader.preferences.background.BackgroundPreference;
import org.geometerplus.android.fbreader.preferences.fileChooser.FileChooserCollection;
import org.geometerplus.android.fbreader.preferences.menu.MenuPreference;
import org.geometerplus.android.fbreader.sync.SyncOperations;

import org.geometerplus.android.util.*;

import org.fbreader.common.android.FBSettingsActivity;

public class PreferenceActivity extends FBSettingsActivity {
	public static String SCREEN_KEY = "screen";

	private class Fragment extends PreferenceFragment {
		private final HashMap<String,Screen> myScreenMap = new HashMap<String,Screen>();

		private class Screen {
			public final ZLResource Resource;
			private final PreferenceScreen myScreen;

			private Screen(ZLResource root, String resourceKey) {
				Resource = root.getResource(resourceKey);
				myScreen = getPreferenceManager().createPreferenceScreen(PreferenceActivity.this);
				myScreen.setTitle(Resource.getValue());
				myScreen.setSummary(Resource.getResource("summary").getValue());
			}

			public void setSummary(CharSequence summary) {
				myScreen.setSummary(summary);
			}

			public Screen createPreferenceScreen(String resourceKey) {
				Screen screen = new Screen(Resource, resourceKey);
				myScreen.addPreference(screen.myScreen);
				return screen;
			}

			public Preference addPreference(Preference preference) {
				myScreen.addPreference(preference);
				return preference;
			}

			public Preference addOption(ZLBooleanOption option, String resourceKey) {
				return addPreference(
					new ZLBooleanPreference(PreferenceActivity.this, option, Resource.getResource(resourceKey))
				);
			}

			public Preference addOption(ZLColorOption option, String resourceKey) {
				return addPreference(
					new ZLColorPreference(PreferenceActivity.this, Resource, resourceKey, option)
				);
			}

			public Preference addOption(ZLIntegerRangeOption option, String resourceKey) {
				return addPreference(new ZLIntegerRangePreference(
					PreferenceActivity.this, Resource.getResource(resourceKey), option
				));
			}

			public <T extends Enum<T>> Preference addOption(ZLEnumOption<T> option, String key) {
				return addPreference(
					new ZLEnumPreference<T>(PreferenceActivity.this, option, Resource.getResource(key))
				);
			}

			public <T extends Enum<T>> Preference addOption(ZLEnumOption<T> option, String key, String valuesKey) {
				return addPreference(
					new ZLEnumPreference<T>(PreferenceActivity.this, option, Resource.getResource(key), Resource.getResource(valuesKey))
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
					final LinearLayout ll = (LinearLayout)view;
					final Toolbar toolbar =
						(Toolbar)getLayoutInflater().inflate(R.layout.md_toolbar, ll, false);
					toolbar.setNavigationOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							dialog.dismiss();
						}
					});
					setupToolbarAppearance(toolbar, true);
					toolbar.setTitle(preference.getTitle());
					ll.addView(toolbar, 0);
				}
			}

			return result;
		}

		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);

			myScreen = getPreferenceManager().createPreferenceScreen(PreferenceActivity.this);

			final Intent intent = getIntent();
			final Uri data = intent.getData();
			final String screenId;
			if (Intent.ACTION_VIEW.equals(intent.getAction())
					&& data != null && "fbreader-preferences".equals(data.getScheme())) {
				screenId = data.getEncodedSchemeSpecificPart();
			} else {
				screenId = intent.getStringExtra(SCREEN_KEY);
			}

			Config.Instance().runOnConnect(new Runnable() {
				public void run() {
					init(intent);
					final Screen screen = myScreenMap.get(screenId);
					setPreferenceScreen(screen != null ? screen.myScreen : myScreen);
					if (screen != null) {
						setTitle(screen.Resource.getValue());
					}
				}
			});
		}

		private void init(Intent intent) {
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
			setResult(FBReader.RESULT_REPAINT);

			final ViewOptions viewOptions = new ViewOptions();
			final MiscOptions miscOptions = new MiscOptions();
			final FooterOptions footerOptions = viewOptions.getFooterOptions();
			final PageTurningOptions pageTurningOptions = new PageTurningOptions();
			final ImageOptions imageOptions = new ImageOptions();
			final SyncOptions syncOptions = new SyncOptions();
			final ColorProfile profile = viewOptions.getColorProfile();
			final ZLTextStyleCollection collection = viewOptions.getTextStyleCollection();
			final ZLKeyBindings keyBindings = new ZLKeyBindings();

			final ZLAndroidLibrary androidLibrary = FBReaderUtil.getZLibrary(PreferenceActivity.this);
			final String decimalSeparator = String.valueOf(
				new DecimalFormatSymbols(Language.uiLocale()).getDecimalSeparator()
			);

			final Screen directoriesScreen = createPreferenceScreen("directories");
			final Runnable libraryUpdater = new Runnable() {
				public void run() {
					final BookCollectionShadow bookCollection = new BookCollectionShadow();
					bookCollection.bindToService(PreferenceActivity.this, new Runnable() {
						public void run() {
							bookCollection.reset(false);
							bookCollection.unbind();
						}
					});
				}
			};
			directoriesScreen.addPreference(myChooserCollection.createPreference(
				directoriesScreen.Resource, "bookPath", Paths.BookPathOption, libraryUpdater
			));
			directoriesScreen.addPreference(myChooserCollection.createPreference(
				directoriesScreen.Resource, "downloadDir", Paths.DownloadsDirectoryOption, libraryUpdater
			));
			final PreferenceSet fontReloader = new PreferenceSet.Reloader();
			directoriesScreen.addPreference(myChooserCollection.createPreference(
				directoriesScreen.Resource, "fontPath", Paths.FontPathOption, fontReloader
			));
			directoriesScreen.addPreference(myChooserCollection.createPreference(
				directoriesScreen.Resource, "tempDir", Paths.TempDirectoryOption(PreferenceActivity.this), null
			));

			final Screen syncScreen = createPreferenceScreen("sync");
			final PreferenceSet syncPreferences = new PreferenceSet.Enabler() {
				@Override
				protected Boolean detectState() {
					return syncOptions.Enabled.getValue();
				}
			};
			syncScreen.addPreference(new UrlPreference(PreferenceActivity.this, syncScreen.Resource, "site"));
			syncScreen.addPreference(new ZLCheckBoxPreference(
				PreferenceActivity.this, syncScreen.Resource.getResource("enable")
			) {
				{
					if (syncOptions.Enabled.getValue()) {
						setChecked(true);
						setOnSummary(SyncUtil.getAccountName(myNetworkContext));
					} else {
						setChecked(false);
					}
				}

				private void enableSynchronisation() {
					SyncOperations.enableSync(PreferenceActivity.this, syncOptions);
				}

				@Override
				protected void onClick() {
					super.onClick();
					syncPreferences.run();

					if (!isChecked()) {
						SyncUtil.logout(myNetworkContext);
						syncOptions.Enabled.setValue(false);
						enableSynchronisation();
						syncPreferences.run();
						new SyncData().reset();
						return;
					}

					UIUtil.createExecutor(PreferenceActivity.this, "tryConnect").execute(new Runnable() {
						public void run() {
							try {
								myNetworkContext.perform(
									new JsonRequest(SyncOptions.BASE_URL + "login/test") {
										@Override
										public void processResponse(Object response) {
											final String account = (String)((Map)response).get("user");
											syncOptions.Enabled.setValue(account != null);
											enableSynchronisation();
											runOnUiThread(new Runnable() {
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
								runOnUiThread(new Runnable() {
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
					runOnUiThread(new Runnable() {
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

			final Screen appearanceScreen = createPreferenceScreen("appearance");
			appearanceScreen.addPreference(new LanguagePreference(
				PreferenceActivity.this, appearanceScreen.Resource.getResource("language"), ZLResource.interfaceLanguages()
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
						finish();
						startActivity(new Intent(
							Intent.ACTION_VIEW, Uri.parse("fbreader-action:preferences#appearance")
						));
					}
				}
			});
			appearanceScreen.addPreference(new SingleChoiceStringPreference(
				PreferenceActivity.this, appearanceScreen.Resource.getResource("screenOrientation"),
				androidLibrary.getOrientationOption(), androidLibrary.allOrientations()
			));
			appearanceScreen.addPreference(new ZLBooleanPreference(
				PreferenceActivity.this,
				viewOptions.TwoColumnView,
				appearanceScreen.Resource.getResource("twoColumnView")
			));
			appearanceScreen.addPreference(new ZLBooleanPreference(
				PreferenceActivity.this,
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
				PreferenceActivity.this,
				androidLibrary.BatteryLevelToTurnScreenOffOption,
				appearanceScreen.Resource.getResource("dontTurnScreenOff")
			));
			/*
			appearanceScreen.addPreference(new ZLBooleanPreference(
				PreferenceActivity.this,
				androidLibrary.DontTurnScreenOffDuringChargingOption,
				appearanceScreen.Resource.getResource("dontTurnScreenOffDuringCharging")
			));
			 */
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				appearanceScreen.addOption(androidLibrary.ShowStatusBarOption, "showStatusBar");
			}
			appearanceScreen.addOption(androidLibrary.ShowActionBarOption, "showActionBar");
			if (Build.VERSION.SDK_INT >= 19/*Build.VERSION_CODES.KITKAT*/) {
				appearanceScreen.addOption(androidLibrary.EnableFullscreenModeOption, "fullscreenMode");
			}
			appearanceScreen.addOption(androidLibrary.DisableButtonLightsOption, "disableButtonLights");

			if (DeviceType.Instance().isEInk()) {
				final EInkOptions einkOptions = new EInkOptions();
				final Screen einkScreen = createPreferenceScreen("eink");
				final PreferenceSet einkPreferences = new PreferenceSet.Enabler() {
					@Override
					protected Boolean detectState() {
						return einkOptions.EnableFastRefresh.getValue();
					}
				};

				einkScreen.addPreference(new ZLBooleanPreference(
					PreferenceActivity.this, einkOptions.EnableFastRefresh,
					einkScreen.Resource.getResource("enableFastRefresh")
				) {
					@Override
					protected void onClick() {
						super.onClick();
						einkPreferences.run();
					}
				});

				final ZLIntegerRangePreference updateIntervalPreference = new ZLIntegerRangePreference(
					PreferenceActivity.this, einkScreen.Resource.getResource("interval"), einkOptions.UpdateInterval
				);
				einkScreen.addPreference(updateIntervalPreference);

				einkPreferences.add(updateIntervalPreference);
				einkPreferences.run();
			}

			final Screen textScreen = createPreferenceScreen("text");

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
				PreferenceActivity.this, textScreen.Resource.getResource("font"),
				baseStyle.FontFamilyOption, false
			)));
			textScreen.addPreference(new ZLIntegerRangePreference(
				PreferenceActivity.this, textScreen.Resource.getResource("fontSize"),
				baseStyle.FontSizeOption
			));
			textScreen.addPreference(new FontStylePreference(
				PreferenceActivity.this, textScreen.Resource.getResource("fontStyle"),
				baseStyle.BoldOption, baseStyle.ItalicOption
			));
			final ZLIntegerRangeOption spaceOption = baseStyle.LineSpaceOption;
			final String[] spacings = new String[spaceOption.MaxValue - spaceOption.MinValue + 1];
			for (int i = 0; i < spacings.length; ++i) {
				final int val = spaceOption.MinValue + i;
				spacings[i] = (char)(val / 10 + '0') + decimalSeparator + (char)(val % 10 + '0');
			}
			textScreen.addPreference(new SingleChoiceIntegerPreference(
				PreferenceActivity.this, textScreen.Resource.getResource("lineSpacing"),
				spaceOption, spacings
			));
			final String[] alignments = { "left", "right", "center", "justify" };
			textScreen.addPreference(new SingleChoiceIntegerPreference(
				PreferenceActivity.this, textScreen.Resource.getResource("alignment"),
				baseStyle.AlignmentOption, alignments
			));
			textScreen.addOption(baseStyle.AutoHyphenationOption, "autoHyphenations");

			final Screen moreStylesScreen = textScreen.createPreferenceScreen("more");
			for (ZLTextNGStyleDescription description : collection.getDescriptionList()) {
				final Screen ngScreen = moreStylesScreen.createPreferenceScreen(description.Name);
				ngScreen.addPreference(new FontPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("font"),
					description.FontFamilyOption, true
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.FontSizeOption,
					StringPreference.CONSTRAINT_POSITIVE_LENGTH,
					textScreen.Resource, "fontSize"
				));
				ngScreen.addPreference(new SingleChoiceStringPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("bold"),
					description.FontWeightOption,
					new String[] { "inherit", "normal", "bold" }
				));
				ngScreen.addPreference(new SingleChoiceStringPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("italic"),
					description.FontStyleOption,
					new String[] { "inherit", "normal", "italic" }
				));
				ngScreen.addPreference(new SingleChoiceStringPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("textDecoration"),
					description.TextDecorationOption,
					new String[] { "inherit", "none", "underline", "line-through" }
				));
				ngScreen.addPreference(new SingleChoiceStringPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("allowHyphenations"),
					description.HyphenationOption,
					new String[] { "inherit", "none", "auto" }
				));
				ngScreen.addPreference(new SingleChoiceStringPreference(
					PreferenceActivity.this, textScreen.Resource.getResource("alignment"),
					description.AlignmentOption,
					new String[] { "inherit", "left", "right", "center", "justify" }
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.LineHeightOption,
					StringPreference.CONSTRAINT_PERCENT,
					textScreen.Resource, "lineSpacing"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.MarginTopOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "spaceBefore"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.MarginBottomOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "spaceAfter"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.MarginLeftOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "leftIndent"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.MarginRightOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "rightIndent"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.TextIndentOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "firstLineIndent"
				));
				ngScreen.addPreference(new StringPreference(
					PreferenceActivity.this, description.VerticalAlignOption,
					StringPreference.CONSTRAINT_LENGTH,
					textScreen.Resource, "verticalAlignment"
				));
			}

			final Screen toastsScreen = createPreferenceScreen("toast");
			toastsScreen.addOption(miscOptions.ToastFontSizePercent, "fontSizePercent");
			toastsScreen.addOption(miscOptions.ShowFootnoteToast, "showFootnoteToast");
			toastsScreen.addPreference(new ZLEnumPreference(
				PreferenceActivity.this,
				miscOptions.FootnoteToastDuration,
				toastsScreen.Resource.getResource("footnoteToastDuration"),
				ZLResource.resource("duration")
			));

			final Screen cssScreen = createPreferenceScreen("css");
			cssScreen.addOption(baseStyle.UseCSSFontFamilyOption, "fontFamily");
			cssScreen.addOption(baseStyle.UseCSSFontSizeOption, "fontSize");
			cssScreen.addOption(baseStyle.UseCSSTextAlignmentOption, "textAlignment");
			cssScreen.addOption(baseStyle.UseCSSMarginsOption, "margins");

			final Screen colorsScreen = createPreferenceScreen("colors");

			final PreferenceSet backgroundSet = new PreferenceSet.Enabler() {
				@Override
				protected Boolean detectState() {
					return profile.WallpaperOption.getValue().startsWith("/");
				}
			};
			myBackgroundPreference = new BackgroundPreference(
				PreferenceActivity.this,
				profile,
				colorsScreen.Resource.getResource("background"),
				BACKGROUND_REQUEST_CODE
			) {
				@Override
				public void update(Intent data) {
					super.update(data);
					backgroundSet.run();
				}
			};
			colorsScreen.addPreference(myBackgroundPreference);
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

			final Screen marginsScreen = createPreferenceScreen("margins");
			marginsScreen.addOption(viewOptions.LeftMargin, "left");
			marginsScreen.addOption(viewOptions.RightMargin, "right");
			marginsScreen.addOption(viewOptions.TopMargin, "top");
			marginsScreen.addOption(viewOptions.BottomMargin, "bottom");
			marginsScreen.addOption(viewOptions.SpaceBetweenColumns, "spaceBetweenColumns");

			final Screen statusLineScreen = createPreferenceScreen("scrollBar");

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
				PreferenceActivity.this, statusLineScreen.Resource.getResource("scrollbarType"),
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
				PreferenceActivity.this, statusLineScreen.Resource.getResource("footerHeight"),
				viewOptions.FooterHeight
			)));
			oldStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterFillOption, "footerOldStyleColor"));
			newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGBackgroundOption, "footerBackgroundColor"));
			newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGForegroundOption, "footerForegroundColor"));
			newStyleFooterPreferences.add(statusLineScreen.addOption(profile.FooterNGForegroundUnreadOption, "footerForegroundUnreadColor"));
			footerPreferences.add(statusLineScreen.addPreference(new ZLBooleanPreference(
				PreferenceActivity.this,
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
				PreferenceActivity.this, statusLineScreen.Resource.getResource("font"),
				footerOptions.Font, false
			)));
			footerPreferences.run();
			tocPreferences.run();
			oldStyleFooterPreferences.run();
			newStyleFooterPreferences.run();

			/*
			final Screen colorProfileScreen = createPreferenceScreen("colorProfile");
			final ZLResource resource = colorProfileScreen.Resource;
			colorProfileScreen.setSummary(ColorProfilePreference.createTitle(resource, fbreader.getColorProfileName()));
			for (String key : ColorProfile.names()) {
				colorProfileScreen.addPreference(new ColorProfilePreference(
					PreferenceActivity.this, fbreader, colorProfileScreen, key, ColorProfilePreference.createTitle(resource, key)
				));
			}
			 */

			final Screen scrollingScreen = createPreferenceScreen("scrolling");
			scrollingScreen.addOption(pageTurningOptions.FingerScrolling, "fingerScrolling");
			scrollingScreen.addOption(miscOptions.EnableDoubleTap, "enableDoubleTapDetection");

			final PreferenceSet volumeKeysPreferences = new PreferenceSet.Enabler() {
				@Override
				protected Boolean detectState() {
					return keyBindings.hasBinding(KeyEvent.KEYCODE_VOLUME_UP, false);
				}
			};
			scrollingScreen.addPreference(new ZLCheckBoxPreference(
				PreferenceActivity.this, scrollingScreen.Resource.getResource("volumeKeys")
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
				PreferenceActivity.this, scrollingScreen.Resource.getResource("invertVolumeKeys")
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
				PreferenceActivity.this,
				scrollingScreen.Resource,
				"animationSpeed",
				pageTurningOptions.AnimationSpeed
			));
			scrollingScreen.addOption(pageTurningOptions.Horizontal, "horizontal");

			final Screen dictionaryScreen = createPreferenceScreen("dictionary");

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
				PreferenceActivity.this, dictionaryScreen.Resource.getResource("targetLanguage"), languages
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

			DictionaryUtil.init(PreferenceActivity.this, new Runnable() {
				public void run() {
					dictionaryScreen.addPreference(new DictionaryPreference(
						PreferenceActivity.this,
						dictionaryScreen.Resource.getResource("dictionary"),
						DictionaryUtil.singleWordTranslatorOption(),
						DictionaryUtil.dictionaryInfos(PreferenceActivity.this, true)
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
						PreferenceActivity.this,
						dictionaryScreen.Resource.getResource("translator"),
						DictionaryUtil.multiWordTranslatorOption(),
						DictionaryUtil.dictionaryInfos(PreferenceActivity.this, false)
					));
					dictionaryScreen.addPreference(new ZLBooleanPreference(
						PreferenceActivity.this,
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

			final Screen imagesScreen = createPreferenceScreen("images");
			imagesScreen.addOption(imageOptions.TapAction, "longTapAction");
			imagesScreen.addOption(imageOptions.FitToScreen, "fitImagesToScreen");
			imagesScreen.addOption(imageOptions.ImageViewBackground, "backgroundColor");
			imagesScreen.addOption(imageOptions.MatchBackground, "matchBackground");

			final Screen menuScreen = createPreferenceScreen("menu");
			menuScreen.addPreference(new MenuPreference(
				PreferenceActivity.this,
				menuScreen.Resource.getResource("items"))
			);
			menuScreen.addOption(miscOptions.CoverAsMenuBackground, "backgroundCover");

			final CancelMenuHelper cancelMenuHelper = new CancelMenuHelper();
			final Screen cancelMenuScreen = createPreferenceScreen("cancelMenu");
			cancelMenuScreen.addOption(cancelMenuHelper.ShowLibraryItemOption, "library");
			cancelMenuScreen.addOption(cancelMenuHelper.ShowNetworkLibraryItemOption, "networkLibrary");
			cancelMenuScreen.addOption(cancelMenuHelper.ShowPreviousBookItemOption, "previousBook");
			cancelMenuScreen.addOption(cancelMenuHelper.ShowPositionItemsOption, "positions");
			final String[] backKeyActions =
				{ ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU };
			cancelMenuScreen.addPreference(new SingleChoiceStringPreference(
				PreferenceActivity.this, cancelMenuScreen.Resource.getResource("backKeyAction"),
				keyBindings.getOption(KeyEvent.KEYCODE_BACK, false), backKeyActions
			));
			final String[] backKeyLongPressActions =
				{ ActionCode.EXIT, ActionCode.SHOW_CANCEL_MENU, FBReaderApp.NoAction };
			cancelMenuScreen.addPreference(new SingleChoiceStringPreference(
				PreferenceActivity.this, cancelMenuScreen.Resource.getResource("backKeyLongPressAction"),
				keyBindings.getOption(KeyEvent.KEYCODE_BACK, true), backKeyLongPressActions
			));

			final Screen tipsScreen = createPreferenceScreen("tips");
			tipsScreen.addOption(TipsManager.ShowTipsOption, "showTips");

			final Screen aboutScreen = createPreferenceScreen("about");
			aboutScreen.addPreference(new InfoPreference(
				PreferenceActivity.this,
				aboutScreen.Resource.getResource("version").getValue(),
				androidLibrary.getFullVersionName()
			));
			aboutScreen.addPreference(new UrlPreference(PreferenceActivity.this, aboutScreen.Resource, "site"));
			aboutScreen.addPreference(new UrlPreference(PreferenceActivity.this, aboutScreen.Resource, "email"));
			aboutScreen.addPreference(new UrlPreference(PreferenceActivity.this, aboutScreen.Resource, "googleplus"));
			aboutScreen.addPreference(new UrlPreference(PreferenceActivity.this, aboutScreen.Resource, "twitter"));
			aboutScreen.addPreference(new UrlPreference(PreferenceActivity.this, aboutScreen.Resource, "facebook"));
			aboutScreen.addPreference(new ThirdPartyLibrariesPreference(PreferenceActivity.this, aboutScreen.Resource, "thirdParty"));
		}

		Screen createPreferenceScreen(String resourceKey) {
			final Screen screen = new Screen(Resource, resourceKey);
			myScreenMap.put(resourceKey, screen);
			myScreen.addPreference(screen.myScreen);
			return screen;
		}
	}

	private static final int BACKGROUND_REQUEST_CODE = 3000;

	private final ActivityNetworkContext myNetworkContext = new ActivityNetworkContext(this);
	private final FileChooserCollection myChooserCollection = new FileChooserCollection(this, 2000);
	private BackgroundPreference myBackgroundPreference;

	private PreferenceScreen myScreen;
	final ZLResource Resource = ZLResource.resource("Preferences");

	public Preference addPreference(Preference preference) {
		myScreen.addPreference(preference);
		return preference;
	}

	@Override
	protected void onResume() {
		super.onResume();
		myNetworkContext.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (myNetworkContext.onActivityResult(requestCode, resultCode, data)) {
			return;
		}

		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
			default:
				myChooserCollection.update(requestCode, data);
				break;
			case BACKGROUND_REQUEST_CODE:
				if (myBackgroundPreference != null) {
					myBackgroundPreference.update(data);
				}
				break;
		}
	}

	@Override
	protected PreferenceFragment preferenceFragment() {
		return new Fragment();
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(Resource.getValue());
		SQLiteCookieDatabase.init(this);
	}
}
