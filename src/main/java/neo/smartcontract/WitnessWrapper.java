package neo.smartcontract;

import java.util.Arrays;

import neo.UInt160;
import neo.network.p2p.payloads.IVerifiable;
import neo.persistence.Snapshot;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WitnessWrapper
 * @Package neo.smartcontract
 * @Description: Witness封装类
 * @date Created in 14:29 2019/3/12
 */
public class WitnessWrapper {
    public byte[] verificationScript;

    public WitnessWrapper(byte[] verificationScript) {
        this.verificationScript = verificationScript;
    }

    public static WitnessWrapper[] create(IVerifiable verifiable, Snapshot snapshot) {
        //LINQ START
/*        WitnessWrapper[] wrappers = verifiable.Witnesses.Select(p => new WitnessWrapper
        {
            VerificationScript = p.VerificationScript
        }).ToArray();*/
        WitnessWrapper[] wrappers = Arrays.asList(verifiable.getWitnesses()).stream()
                .map(p -> new WitnessWrapper(p.verificationScript)).toArray(WitnessWrapper[]::new);
        //LINQ END
        //LINQ START
/*        if (wrappers.Any(p => p.VerificationScript.Length == 0))
        {
            UInt160[] hashes = verifiable.GetScriptHashesForVerifying(snapshot);
            for (int i = 0; i < wrappers.Length; i++)
                if (wrappers[i].VerificationScript.Length == 0)
                    wrappers[i].VerificationScript = snapshot.Contracts[hashes[i]].Script;
        }*/

        if (Arrays.asList(wrappers).stream().anyMatch(p -> p.verificationScript.length == 0)) {
            UInt160[] hashes = verifiable.getScriptHashesForVerifying(snapshot);
            for (int i = 0; i < wrappers.length; i++) {
                if (wrappers[i].verificationScript.length == 0) {
                    wrappers[i].verificationScript = snapshot.getContracts().get(hashes[i]).script;
                }
            }
        }
        //LINQ END
        return wrappers;
    }
}