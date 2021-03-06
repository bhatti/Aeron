package uk.co.real_logic.aeron.common.uri;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for Aeron uri used for configuring channels.  The format is:
 *
 * <pre>
 * aeron-uri = "aeron:" media [ "?" param *( "|" param ) ]
 * media     = *( "[^?:]" )
 * param     = key "=" value
 * key       = *( "[^=]" )
 * value     = *( "[^|]" )
 * </pre>
 *
 * <li>Multiple params with the same key are allowed, the last value specified 'wins'.</li>
 */
public class AeronUri
{
    private static final String AERON_PREFIX = "aeron:";
    private final String scheme;
    private final String media;
    private final Map<String, String> params;

    public AeronUri(String scheme, String media, Map<String, String> params)
    {
        this.scheme = scheme;
        this.media = media;
        this.params = params;
    }

    public String getMedia()
    {
        return media;
    }

    public String getScheme()
    {
        return scheme;
    }

    private enum State
    {
        MEDIA, PARAMS_KEY, PARAMS_VALUE
    }

    public String get(String key)
    {
        return params.get(key);
    }

    public String get(String key, String defaultValue)
    {
        final String value = params.get(key);

        if (null != value)
        {
            return value;
        }

        return defaultValue;
    }

    public static AeronUri parse(CharSequence cs)
    {
        if (!startsWith(cs, AERON_PREFIX))
        {
            throw new IllegalArgumentException("AeronUri must start with 'aeron:', found: '" + cs + "'");
        }

        final StringBuilder builder = new StringBuilder();

        final String scheme = "aeron";
        final Map<String, String> params = new HashMap<>();
        String media = null;
        String key = null;

        State state = State.MEDIA;
        for (int i = AERON_PREFIX.length(); i < cs.length(); i++)
        {
            final char c = cs.charAt(i);

            switch (state)
            {
            case MEDIA:
                switch (c)
                {
                case '?':
                    media = builder.toString();
                    builder.setLength(0);
                    state = State.PARAMS_KEY;
                    break;

                case ':':
                    throw new IllegalArgumentException("Encountered ':' within media definition");

                default:
                    builder.append(c);
                }
                break;

            case PARAMS_KEY:
                switch (c)
                {
                case '=':
                    key = builder.toString();
                    builder.setLength(0);
                    state = State.PARAMS_VALUE;
                    break;

                default:
                    builder.append(c);
                }
                break;

            case PARAMS_VALUE:
                switch (c)
                {
                case '|':
                    params.put(key, builder.toString());
                    builder.setLength(0);
                    state = State.PARAMS_KEY;
                    break;

                default:
                    builder.append(c);
                }
                break;

            default:
                throw new IllegalStateException("Que?  State = " + state);
            }
        }

        switch (state)
        {
        case MEDIA:
            media = builder.toString();
            break;

        case PARAMS_VALUE:
            params.put(key, builder.toString());
            break;

        default:
            throw new IllegalArgumentException("No more input found, but was in state: " + state);
        }

        return new AeronUri(scheme, media, params);
    }

    private static boolean startsWith(CharSequence input, CharSequence prefix)
    {
        if (input.length() < prefix.length())
        {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++)
        {
            if (input.charAt(i) != prefix.charAt(i))
            {
                return false;
            }
        }

        return true;
    }
}
