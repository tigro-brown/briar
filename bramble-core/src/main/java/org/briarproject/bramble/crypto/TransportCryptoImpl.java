package org.briarproject.bramble.crypto;

import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.crypto.TransportCrypto;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.transport.IncomingKeys;
import org.briarproject.bramble.api.transport.OutgoingKeys;
import org.briarproject.bramble.api.transport.StaticTransportKeys;
import org.briarproject.bramble.api.transport.TransportKeys;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.Blake2bDigest;

import javax.inject.Inject;

import static java.lang.System.arraycopy;
import static org.briarproject.bramble.api.transport.TransportConstants.ALICE_HEADER_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.ALICE_STATIC_HEADER_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.ALICE_STATIC_TAG_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.ALICE_TAG_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.BOB_HEADER_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.BOB_STATIC_HEADER_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.BOB_STATIC_TAG_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.BOB_TAG_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.ROTATE_LABEL;
import static org.briarproject.bramble.api.transport.TransportConstants.TAG_LENGTH;
import static org.briarproject.bramble.util.ByteUtils.INT_16_BYTES;
import static org.briarproject.bramble.util.ByteUtils.INT_64_BYTES;
import static org.briarproject.bramble.util.ByteUtils.MAX_16_BIT_UNSIGNED;
import static org.briarproject.bramble.util.ByteUtils.MAX_32_BIT_UNSIGNED;
import static org.briarproject.bramble.util.ByteUtils.writeUint16;
import static org.briarproject.bramble.util.ByteUtils.writeUint64;
import static org.briarproject.bramble.util.StringUtils.toUtf8;

class TransportCryptoImpl implements TransportCrypto {

	private final CryptoComponent crypto;

	@Inject
	TransportCryptoImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public TransportKeys deriveTransportKeys(TransportId t,
			SecretKey rootKey, long timePeriod, boolean alice, boolean active) {
		// Keys for the previous period are derived from the root key
		SecretKey inTagPrev = deriveTagKey(rootKey, t, !alice);
		SecretKey inHeaderPrev = deriveHeaderKey(rootKey, t, !alice);
		SecretKey outTagPrev = deriveTagKey(rootKey, t, alice);
		SecretKey outHeaderPrev = deriveHeaderKey(rootKey, t, alice);
		// Derive the keys for the current and next periods
		SecretKey inTagCurr = rotateKey(inTagPrev, timePeriod);
		SecretKey inHeaderCurr = rotateKey(inHeaderPrev, timePeriod);
		SecretKey inTagNext = rotateKey(inTagCurr, timePeriod + 1);
		SecretKey inHeaderNext = rotateKey(inHeaderCurr, timePeriod + 1);
		SecretKey outTagCurr = rotateKey(outTagPrev, timePeriod);
		SecretKey outHeaderCurr = rotateKey(outHeaderPrev, timePeriod);
		// Initialise the reordering windows and stream counters
		IncomingKeys inPrev = new IncomingKeys(inTagPrev, inHeaderPrev,
				timePeriod - 1);
		IncomingKeys inCurr = new IncomingKeys(inTagCurr, inHeaderCurr,
				timePeriod);
		IncomingKeys inNext = new IncomingKeys(inTagNext, inHeaderNext,
				timePeriod + 1);
		OutgoingKeys outCurr = new OutgoingKeys(outTagCurr, outHeaderCurr,
				timePeriod, active);
		// Collect and return the keys
		return new TransportKeys(t, inPrev, inCurr, inNext, outCurr);
	}

	@Override
	public TransportKeys rotateTransportKeys(TransportKeys k, long timePeriod) {
		if (k.getTimePeriod() >= timePeriod) return k;
		IncomingKeys inPrev = k.getPreviousIncomingKeys();
		IncomingKeys inCurr = k.getCurrentIncomingKeys();
		IncomingKeys inNext = k.getNextIncomingKeys();
		OutgoingKeys outCurr = k.getCurrentOutgoingKeys();
		long startPeriod = outCurr.getTimePeriod();
		boolean active = outCurr.isActive();
		// Rotate the keys
		for (long p = startPeriod + 1; p <= timePeriod; p++) {
			inPrev = inCurr;
			inCurr = inNext;
			SecretKey inNextTag = rotateKey(inNext.getTagKey(), p + 1);
			SecretKey inNextHeader = rotateKey(inNext.getHeaderKey(), p + 1);
			inNext = new IncomingKeys(inNextTag, inNextHeader, p + 1);
			SecretKey outCurrTag = rotateKey(outCurr.getTagKey(), p);
			SecretKey outCurrHeader = rotateKey(outCurr.getHeaderKey(), p);
			outCurr = new OutgoingKeys(outCurrTag, outCurrHeader, p, active);
		}
		// Collect and return the keys
		return new TransportKeys(k.getTransportId(), inPrev, inCurr, inNext,
				outCurr);
	}

	private SecretKey rotateKey(SecretKey k, long timePeriod) {
		byte[] period = new byte[INT_64_BYTES];
		writeUint64(timePeriod, period, 0);
		return crypto.deriveKey(ROTATE_LABEL, k, period);
	}

	private SecretKey deriveTagKey(SecretKey rootKey, TransportId t,
			boolean alice) {
		String label = alice ? ALICE_TAG_LABEL : BOB_TAG_LABEL;
		byte[] id = toUtf8(t.getString());
		return crypto.deriveKey(label, rootKey, id);
	}

	private SecretKey deriveHeaderKey(SecretKey rootKey, TransportId t,
			boolean alice) {
		String label = alice ? ALICE_HEADER_LABEL : BOB_HEADER_LABEL;
		byte[] id = toUtf8(t.getString());
		return crypto.deriveKey(label, rootKey, id);
	}

	@Override
	public StaticTransportKeys deriveStaticTransportKeys(TransportId t,
			SecretKey rootKey, boolean alice, long timePeriod) {
		if (timePeriod < 1) throw new IllegalArgumentException();
		IncomingKeys inPrev = deriveStaticIncomingKeys(t, rootKey, alice,
				timePeriod - 1);
		IncomingKeys inCurr = deriveStaticIncomingKeys(t, rootKey, alice,
				timePeriod);
		IncomingKeys inNext = deriveStaticIncomingKeys(t, rootKey, alice,
				timePeriod + 1);
		OutgoingKeys outCurr = deriveStaticOutgoingKeys(t, rootKey, alice,
				timePeriod);
		return new StaticTransportKeys(t, inPrev, inCurr, inNext, outCurr,
				rootKey, alice);
	}

	private IncomingKeys deriveStaticIncomingKeys(TransportId t,
			SecretKey rootKey, boolean alice, long timePeriod) {
		SecretKey tag = deriveStaticTagKey(t, rootKey, !alice, timePeriod);
		SecretKey header = deriveStaticHeaderKey(t, rootKey, !alice,
				timePeriod);
		return new IncomingKeys(tag, header, timePeriod);
	}

	private OutgoingKeys deriveStaticOutgoingKeys(TransportId t,
			SecretKey rootKey, boolean alice, long timePeriod) {
		SecretKey tag = deriveStaticTagKey(t, rootKey, alice, timePeriod);
		SecretKey header = deriveStaticHeaderKey(t, rootKey, alice, timePeriod);
		return new OutgoingKeys(tag, header, timePeriod, true);
	}

	private SecretKey deriveStaticTagKey(TransportId t, SecretKey rootKey,
			boolean alice, long timePeriod) {
		String label = alice ? ALICE_STATIC_TAG_LABEL : BOB_STATIC_TAG_LABEL;
		byte[] id = toUtf8(t.getString());
		byte[] period = new byte[INT_64_BYTES];
		writeUint64(timePeriod, period, 0);
		return crypto.deriveKey(label, rootKey, id, period);
	}

	private SecretKey deriveStaticHeaderKey(TransportId t, SecretKey rootKey,
			boolean alice, long timePeriod) {
		String label =
				alice ? ALICE_STATIC_HEADER_LABEL : BOB_STATIC_HEADER_LABEL;
		byte[] id = toUtf8(t.getString());
		byte[] period = new byte[INT_64_BYTES];
		writeUint64(timePeriod, period, 0);
		return crypto.deriveKey(label, rootKey, id, period);
	}

	@Override
	public StaticTransportKeys updateTransportKeys(StaticTransportKeys k,
			long timePeriod) {
		long elapsed = timePeriod - k.getTimePeriod();
		TransportId t = k.getTransportId();
		SecretKey rootKey = k.getRootKey();
		boolean alice = k.isAlice();
		if (elapsed <= 0) {
			// The keys are for the given period or later - don't update them
			return k;
		} else if (elapsed == 1) {
			// The keys are one period old - shift by one period
			IncomingKeys inPrev = k.getCurrentIncomingKeys();
			IncomingKeys inCurr = k.getNextIncomingKeys();
			IncomingKeys inNext = deriveStaticIncomingKeys(t, rootKey, alice,
					timePeriod + 1);
			OutgoingKeys outCurr = deriveStaticOutgoingKeys(t, rootKey, alice,
					timePeriod);
			return new StaticTransportKeys(t, inPrev, inCurr, inNext, outCurr,
					rootKey, alice);
		} else if (elapsed == 2) {
			// The keys are two periods old - shift by two periods
			IncomingKeys inPrev = k.getNextIncomingKeys();
			IncomingKeys inCurr = deriveStaticIncomingKeys(t, rootKey, alice,
					timePeriod);
			IncomingKeys inNext = deriveStaticIncomingKeys(t, rootKey, alice,
					timePeriod + 1);
			OutgoingKeys outCurr = deriveStaticOutgoingKeys(t, rootKey, alice,
					timePeriod);
			return new StaticTransportKeys(t, inPrev, inCurr, inNext, outCurr,
					rootKey, alice);
		} else {
			// The keys are more than two periods old - derive fresh keys
			return deriveStaticTransportKeys(t, rootKey, alice, timePeriod);
		}
	}

	@Override
	public void encodeTag(byte[] tag, SecretKey tagKey, int protocolVersion,
			long streamNumber) {
		if (tag.length < TAG_LENGTH) throw new IllegalArgumentException();
		if (protocolVersion < 0 || protocolVersion > MAX_16_BIT_UNSIGNED)
			throw new IllegalArgumentException();
		if (streamNumber < 0 || streamNumber > MAX_32_BIT_UNSIGNED)
			throw new IllegalArgumentException();
		// Initialise the PRF
		Digest prf = new Blake2bDigest(tagKey.getBytes(), 32, null, null);
		// The output of the PRF must be long enough to use as a tag
		int macLength = prf.getDigestSize();
		if (macLength < TAG_LENGTH) throw new IllegalStateException();
		// The input is the protocol version as a 16-bit integer, followed by
		// the stream number as a 64-bit integer
		byte[] protocolVersionBytes = new byte[INT_16_BYTES];
		writeUint16(protocolVersion, protocolVersionBytes, 0);
		prf.update(protocolVersionBytes, 0, protocolVersionBytes.length);
		byte[] streamNumberBytes = new byte[INT_64_BYTES];
		writeUint64(streamNumber, streamNumberBytes, 0);
		prf.update(streamNumberBytes, 0, streamNumberBytes.length);
		byte[] mac = new byte[macLength];
		prf.doFinal(mac, 0);
		// The output is the first TAG_LENGTH bytes of the MAC
		arraycopy(mac, 0, tag, 0, TAG_LENGTH);
	}
}
