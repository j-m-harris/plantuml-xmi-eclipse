/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2023, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 * 
 *
 */
package net.sourceforge.plantuml.cucadiagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.BackSlash;
import net.sourceforge.plantuml.EmbeddedDiagram;
import net.sourceforge.plantuml.Guillemet;
import net.sourceforge.plantuml.ISkinSimple;
import net.sourceforge.plantuml.LineBreakStrategy;
import net.sourceforge.plantuml.LineLocationImpl;
import net.sourceforge.plantuml.SpriteContainer;
import net.sourceforge.plantuml.StringLocated;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.UrlBuilder;
import net.sourceforge.plantuml.UrlMode;
import net.sourceforge.plantuml.command.regex.Matcher2;
import net.sourceforge.plantuml.command.regex.MyPattern;
import net.sourceforge.plantuml.command.regex.Pattern2;
import net.sourceforge.plantuml.creole.CreoleMode;
import net.sourceforge.plantuml.creole.Parser;
import net.sourceforge.plantuml.creole.Sheet;
import net.sourceforge.plantuml.creole.SheetBlock1;
import net.sourceforge.plantuml.creole.SheetBlock2;
import net.sourceforge.plantuml.creole.legacy.CreoleParser;
import net.sourceforge.plantuml.graphic.CircledCharacter;
import net.sourceforge.plantuml.graphic.FontConfiguration;
import net.sourceforge.plantuml.graphic.HorizontalAlignment;
import net.sourceforge.plantuml.graphic.TextBlock;
import net.sourceforge.plantuml.graphic.TextBlockSprited;
import net.sourceforge.plantuml.graphic.TextBlockUtils;
import net.sourceforge.plantuml.graphic.VerticalAlignment;
import net.sourceforge.plantuml.sequencediagram.MessageNumber;
import net.sourceforge.plantuml.skin.VisibilityModifier;
import net.sourceforge.plantuml.style.PName;
import net.sourceforge.plantuml.style.Style;
import net.sourceforge.plantuml.style.Value;
import net.sourceforge.plantuml.style.ValueNull;
import net.sourceforge.plantuml.ugraphic.UFont;
import net.sourceforge.plantuml.ugraphic.UStroke;
import net.sourceforge.plantuml.ugraphic.color.HColor;
import net.sourceforge.plantuml.ugraphic.color.NoSuchColorException;

public class Display implements Iterable<CharSequence> {

	private final List<CharSequence> displayData;
	private final HorizontalAlignment naturalHorizontalAlignment;
	private final boolean isNull;
	private final CreoleMode defaultCreoleMode;
	private final boolean showStereotype;

	public final static Display NULL = new Display(true, null, null, true, CreoleMode.FULL);

	public boolean showStereotype() {
		return showStereotype;
	}

	public Display withoutStereotypeIfNeeded(Style usedStyle) {
		if (this == NULL)
			return NULL;

		final Value showStereotype = usedStyle.value(PName.ShowStereotype);
		if (showStereotype instanceof ValueNull || showStereotype.asBoolean())
			return this;

		return new Display(false, this, this.defaultCreoleMode);

	}

	public Stereotype getStereotypeIfAny() {
		for (CharSequence cs : displayData)
			if (cs instanceof Stereotype)
				return (Stereotype) cs;

		return null;

	}

	public Display replaceBackslashT() {
		final Display result = new Display(this.showStereotype, this, defaultCreoleMode);
		for (int i = 0; i < result.displayData.size(); i++) {
			final CharSequence s = displayData.get(i);
			if (s.toString().contains("\\t"))
				result.displayData.set(i, s.toString().replace("\\t", "\t"));
		}
		return result;
	}

	public Display replace(String src, String dest) {
		final List<CharSequence> newDisplay = new ArrayList<>();
		for (CharSequence cs : displayData) {
			if (cs.toString().contains(src))
				cs = cs.toString().replace(src, dest);

			newDisplay.add(cs);
		}
		return new Display(showStereotype, newDisplay, naturalHorizontalAlignment, isNull, defaultCreoleMode);
	}

	public boolean isWhite() {
		return displayData == null || displayData.size() == 0
				|| (displayData.size() == 1 && displayData.get(0).toString().matches("\\s*"));
	}

	public static Display empty() {
		return new Display(true, (HorizontalAlignment) null, false, CreoleMode.FULL);
	}

	public static Display create(CharSequence... s) {
		return create(Arrays.asList(s));
	}

	public static Display createFoo(List<StringLocated> data) throws NoSuchColorException {
		final List<CharSequence> tmp = new ArrayList<>();
		for (StringLocated s : data)
			tmp.add(s.getString());

		final Display result = create(tmp);
		CreoleParser.checkColor(result);
		return result;
	}

	public static Display create(Collection<? extends CharSequence> other) {
		return new Display(true, other, null, false, CreoleMode.FULL);
	}

	public static Display getWithNewlines(Code s) {
		return getWithNewlines(s.getName());
	}

	public static Display getWithNewlines2(String s) throws NoSuchColorException {
		final Display result = getWithNewlines(s);
		CreoleParser.checkColor(result);
		return result;
	}

	public static Display getWithNewlines(String s) {
		if (s == null)
			return NULL;

		final List<String> result = new ArrayList<>();
		final StringBuilder current = new StringBuilder();
		HorizontalAlignment naturalHorizontalAlignment = null;
		boolean rawMode = false;
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			final String sub = s.substring(i);
			if (sub.startsWith("<math>") || sub.startsWith("<latex>") || sub.startsWith("[["))
				rawMode = true;
			else if (sub.startsWith("</math>") || sub.startsWith("</latex>") || sub.startsWith("]]"))
				rawMode = false;

			if (rawMode == false && c == '\\' && i < s.length() - 1) {
				final char c2 = s.charAt(i + 1);
				i++;
				if (c2 == 'n' || c2 == 'r' || c2 == 'l') {
					if (c2 == 'r')
						naturalHorizontalAlignment = HorizontalAlignment.RIGHT;
					else if (c2 == 'l')
						naturalHorizontalAlignment = HorizontalAlignment.LEFT;

					result.add(current.toString());
					current.setLength(0);
				} else if (c2 == 't') {
					current.append('\t');
				} else if (c2 == '\\') {
					current.append(c2);
				} else {
					current.append(c);
					current.append(c2);
				}
			} else if (c == BackSlash.hiddenNewLine()) {
				result.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		result.add(current.toString());
		return new Display(true, result, naturalHorizontalAlignment, false, CreoleMode.FULL);
	}

	private Display(boolean showStereotype, Display other, CreoleMode mode) {
		this(showStereotype, other.naturalHorizontalAlignment, other.isNull, mode);
		this.displayData.addAll(other.displayData);
	}

	private Display(boolean showStereotype, HorizontalAlignment naturalHorizontalAlignment, boolean isNull,
			CreoleMode defaultCreoleMode) {
		this.showStereotype = showStereotype;
		this.defaultCreoleMode = defaultCreoleMode;
		this.isNull = isNull;
		this.displayData = isNull ? null : new ArrayList<CharSequence>();
		this.naturalHorizontalAlignment = isNull ? null : naturalHorizontalAlignment;
	}

	private Display(boolean showStereotype, Collection<? extends CharSequence> other,
			HorizontalAlignment naturalHorizontalAlignment, boolean isNull, CreoleMode defaultCreoleMode) {
		this(showStereotype, naturalHorizontalAlignment, isNull, defaultCreoleMode);
		if (isNull == false)
			this.displayData.addAll(manageEmbeddedDiagrams(other));

	}

	private static List<CharSequence> manageEmbeddedDiagrams(final Collection<? extends CharSequence> strings) {
		final List<CharSequence> result = new ArrayList<>();
		final Iterator<? extends CharSequence> it = strings.iterator();
		while (it.hasNext()) {
			CharSequence s = it.next();
			final String type = EmbeddedDiagram.getEmbeddedType(s);
			if (type != null) {
				final List<CharSequence> other = new ArrayList<>();
				other.add("@start" + type);
				while (it.hasNext()) {
					final CharSequence s2 = it.next();
					if (s2 != null && StringUtils.trin(s2.toString()).equals("}}"))
						break;

					other.add(s2);
				}
				other.add("@end" + type);
				s = new EmbeddedDiagram(Display.create(other));
			}
			result.add(s);
		}
		return result;
	}

	public Display manageGuillemet() {
		final List<CharSequence> result = new ArrayList<>();
		boolean first = true;
		for (CharSequence line : displayData) {
			if (line instanceof EmbeddedDiagram) {
				result.add(line);
			} else {
				String lineString = line.toString();
				if (first && VisibilityModifier.isVisibilityCharacter(line))
					lineString = lineString.substring(1).trim();

				final String withGuillement = Guillemet.GUILLEMET.manageGuillemet(lineString);
				result.add(withGuillement);
			}
			first = false;
		}
		return new Display(showStereotype, result, this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
	}

	public Display withPage(int page, int lastpage) {
		if (displayData == null)
			return this;

		final List<CharSequence> result = new ArrayList<>();
		for (CharSequence line : displayData) {
			line = line.toString().replace("%page%", "" + page);
			line = line.toString().replace("%lastpage%", "" + lastpage);
			result.add(line);
		}
		return new Display(showStereotype, result, this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
	}

	public Display removeEndingStereotype() {
		final Matcher2 m = patternStereotype.matcher(displayData.get(displayData.size() - 1));
		if (m.matches()) {
			final List<CharSequence> result = new ArrayList<>(this.displayData);
			result.set(result.size() - 1, m.group(1));
			return new Display(showStereotype, result, this.naturalHorizontalAlignment, this.isNull,
					this.defaultCreoleMode);
		}
		return this;
	}

	public final static Pattern2 patternStereotype = MyPattern.cmpile("^(.*?)(?:\\<\\<\\s*(.*)\\s*\\>\\>)\\s*$");

	public String getEndingStereotype() {
		final Matcher2 m = patternStereotype.matcher(displayData.get(displayData.size() - 1));
		if (m.matches())
			return m.group(2);

		return null;
	}

	public Display underlined() {
		final List<CharSequence> result = new ArrayList<>();
		for (CharSequence line : displayData)
			result.add("<u>" + line);

		return new Display(showStereotype, result, this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
	}

	public Display underlinedName() {
		final Pattern p = Pattern.compile("^([^:]+?)(\\s*:.+)$");
		final List<CharSequence> result = new ArrayList<>();
		for (CharSequence line : displayData) {
			if (result.size() == 0) {
				final Matcher m = p.matcher(line);
				if (m.matches())
					result.add("<u>" + m.group(1) + "</u>" + m.group(2));
				else
					result.add("<u>" + line);
			} else {
				result.add("<u>" + line);
			}
		}
		return new Display(showStereotype, result, this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
	}

	public Display withCreoleMode(CreoleMode mode) {
		if (isNull)
			throw new IllegalArgumentException();

		return new Display(this.showStereotype, this, mode);
	}

	@Override
	public String toString() {
		if (isNull)
			return "NULL";

		return displayData.toString();
	}

	@Override
	public int hashCode() {
		return displayData.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return this.displayData.equals(((Display) other).displayData);
	}

	public Display addAll(Display other) {
		final Display result = new Display(this.showStereotype, this, this.defaultCreoleMode);
		result.displayData.addAll(other.displayData);
		return result;
	}

	public Display addFirst(CharSequence s) {
		final Display result = new Display(this.showStereotype, this, this.defaultCreoleMode);
		result.displayData.add(0, s);
		return result;
	}

	public Display add(CharSequence s) {
		final Display result = new Display(this.showStereotype, this, this.defaultCreoleMode);
		result.displayData.add(s);
		return result;
	}

	public Display addGeneric(CharSequence s) {
		final Display result = new Display(this.showStereotype, this, this.defaultCreoleMode);
		final int size = displayData.size();
		if (size == 0)
			result.displayData.add("<" + s + ">");
		else
			result.displayData.set(size - 1, displayData.get(size - 1) + "<" + s + ">");

		return result;
	}

	public int size() {
		if (isNull)
			return 0;

		return displayData.size();
	}

	public CharSequence get(int i) {
		return displayData.get(i);
	}

	public ListIterator<CharSequence> iterator() {
		return Collections.unmodifiableList(displayData).listIterator();
	}

	public Display subList(int i, int size) {
		return new Display(showStereotype, displayData.subList(i, size), this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
	}

	public List<? extends CharSequence> asList() {
		return Collections.unmodifiableList(displayData);
	}

	public List<StringLocated> as2() {
		final List<StringLocated> result = new ArrayList<>();
		LineLocationImpl location = new LineLocationImpl("inner", null);
		for (CharSequence cs : displayData) {
			location = location.oneLineRead();
			result.add(new StringLocated(cs.toString(), location));
		}
		return Collections.unmodifiableList(result);
	}

	public boolean hasUrl() {
		final UrlBuilder urlBuilder = new UrlBuilder(null, UrlMode.ANYWHERE);
		for (CharSequence s : this)
			if (urlBuilder.getUrl(s.toString()) != null)
				return true;

		return false;
	}

	public HorizontalAlignment getNaturalHorizontalAlignment() {
		return naturalHorizontalAlignment;
	}

	public List<Display> splitMultiline(Pattern2 separator) {
		final List<Display> result = new ArrayList<>();
		Display pending = new Display(showStereotype, this.naturalHorizontalAlignment, this.isNull,
				this.defaultCreoleMode);
		result.add(pending);
		for (CharSequence line : displayData) {
			final Matcher2 m = separator.matcher(line);
			if (m.find()) {
				final CharSequence s1 = line.subSequence(0, m.start());
				pending.displayData.add(s1);
				final CharSequence s2 = line.subSequence(m.end(), line.length());
				pending = new Display(showStereotype, this.naturalHorizontalAlignment, this.isNull,
						this.defaultCreoleMode);
				result.add(pending);
				pending.displayData.add(s2);
			} else {
				pending.displayData.add(line);
			}
		}
		return Collections.unmodifiableList(result);
	}

	// ------

	public static boolean isNull(Display display) {
		// Objects.requireNonNull(display);
		return display == null || display.isNull;
	}

	public TextBlock create(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer) {
		return create7(fontConfiguration, horizontalAlignment, spriteContainer, CreoleMode.FULL);
	}

	public TextBlock createWithNiceCreoleMode(FontConfiguration fontConfiguration,
			HorizontalAlignment horizontalAlignment, ISkinSimple spriteContainer) {
		return create7(fontConfiguration, horizontalAlignment, spriteContainer, defaultCreoleMode);
	}

	public TextBlock create7(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, CreoleMode creoleMode) {
		return create0(fontConfiguration, horizontalAlignment, spriteContainer, LineBreakStrategy.NONE, creoleMode,
				null, null);
	}

	public TextBlock create8(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, CreoleMode modeSimpleLine, LineBreakStrategy maxMessageSize) {
		return create0(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize, modeSimpleLine, null,
				null);
	}

	public TextBlock create9(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, LineBreakStrategy maxMessageSize) {
		return create0(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize, defaultCreoleMode, null,
				null);
	}

	public TextBlock create0(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, LineBreakStrategy maxMessageSize, CreoleMode creoleMode,
			UFont fontForStereotype, HColor htmlColorForStereotype) {
		return create0(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize, creoleMode,
				fontForStereotype, htmlColorForStereotype, 0, 0);
	}

	public TextBlock create0(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, LineBreakStrategy maxMessageSize, CreoleMode creoleMode,
			UFont fontForStereotype, HColor htmlColorForStereotype, double marginX1, double marginX2) {
		Objects.requireNonNull(maxMessageSize);
		if (getNaturalHorizontalAlignment() != null)
			horizontalAlignment = getNaturalHorizontalAlignment();

		final FontConfiguration stereotypeConfiguration = fontConfiguration.forceFont(fontForStereotype,
				htmlColorForStereotype);
		if (size() > 0) {
			if (get(0) instanceof Stereotype)
				return createStereotype(fontConfiguration, horizontalAlignment, spriteContainer, 0, fontForStereotype,
						htmlColorForStereotype, maxMessageSize, creoleMode, marginX1, marginX2);

			if (get(size() - 1) instanceof Stereotype)
				return createStereotype(fontConfiguration, horizontalAlignment, spriteContainer, size() - 1,
						fontForStereotype, htmlColorForStereotype, maxMessageSize, creoleMode, marginX1, marginX2);

			if (get(0) instanceof MessageNumber)
				return createMessageNumber(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize,
						stereotypeConfiguration, marginX1, marginX2);
		}

		return getCreole(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize, creoleMode,
				stereotypeConfiguration, marginX1, marginX2);
	}

	private TextBlock createStereotype(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			SpriteContainer spriteContainer, int position, UFont fontForStereotype, HColor htmlColorForStereotype,
			LineBreakStrategy maxMessageSize, CreoleMode creoleMode, double marginX1, double marginX2) {
		final Stereotype stereotype = (Stereotype) get(position);
		TextBlock circledCharacter = null;
		if (stereotype.isSpotted())
			circledCharacter = new CircledCharacter(stereotype.getCharacter(), stereotype.getRadius(),
					stereotype.getCircledFont(), stereotype.getHtmlColor(), null, fontConfiguration.getColor());
		else
			circledCharacter = stereotype.getSprite(spriteContainer);

		final FontConfiguration stereotypeConfiguration = fontConfiguration.forceFont(fontForStereotype,
				htmlColorForStereotype);
		final TextBlock result = getCreole(fontConfiguration, horizontalAlignment, (ISkinSimple) spriteContainer,
				maxMessageSize, creoleMode, stereotypeConfiguration, marginX1, marginX2);
		if (circledCharacter != null)
			return new TextBlockSprited(circledCharacter, result);

		return result;
	}

	private TextBlock getCreole(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, LineBreakStrategy maxMessageSize, CreoleMode creoleMode,
			FontConfiguration stereotypeConfiguration, double marginX1, double marginX2) {
		final Sheet sheet = Parser
				.build(fontConfiguration, horizontalAlignment, spriteContainer, creoleMode, stereotypeConfiguration)
				.createSheet(this);
		final double padding = spriteContainer == null ? 0 : spriteContainer.getPadding();
		final SheetBlock1 sheetBlock1 = new SheetBlock1(sheet, maxMessageSize, padding, marginX1, marginX2);
		return new SheetBlock2(sheetBlock1, sheetBlock1, new UStroke(1.5));
	}

	private TextBlock createMessageNumber(FontConfiguration fontConfiguration, HorizontalAlignment horizontalAlignment,
			ISkinSimple spriteContainer, LineBreakStrategy maxMessageSize, FontConfiguration stereotypeConfiguration,
			double marginX1, double marginX2) {
		TextBlock tb1 = subList(0, 1).getCreole(fontConfiguration, horizontalAlignment, spriteContainer, maxMessageSize,
				CreoleMode.FULL, stereotypeConfiguration, marginX1, marginX2);
		tb1 = TextBlockUtils.withMargin(tb1, 0, 4, 0, 0);
		final TextBlock tb2 = subList(1, size()).getCreole(fontConfiguration, horizontalAlignment, spriteContainer,
				maxMessageSize, CreoleMode.FULL, stereotypeConfiguration, marginX1, marginX2);
		return TextBlockUtils.mergeLR(tb1, tb2, VerticalAlignment.CENTER);

	}

	public boolean hasSeveralGuideLines() {
		return hasSeveralGuideLines(displayData);
	}

	public static boolean hasSeveralGuideLines(String s) {
		final List<String> splitted = Arrays.asList(s.split("\\\\n"));
		return hasSeveralGuideLines(splitted);
	}

	private static boolean hasSeveralGuideLines(Collection<? extends CharSequence> all) {
		if (all.size() <= 1)
			return false;

		for (CharSequence cs : all) {
			final String s = cs.toString();
			if (s.startsWith("< "))
				return true;

			if (s.startsWith("> "))
				return true;

			if (s.endsWith(" <"))
				return true;

			if (s.endsWith(" >"))
				return true;
		}
		return false;
	}

}
