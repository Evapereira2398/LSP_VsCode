package custom.android.apuracao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoField;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.entities.readonly.IR002FEC;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.entities.readonly.IR070ACC;
import com.senior.rule.Rule;

import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;

@Rule(description = "Regra de Apuração - ANDROID")
public class RegraApuracao extends Apuracao {

    private int iAuxDia;

    @Override
    public void execute() {
        // Obtém contexto da apuração
        ContextoApuracao apuracao = getContainer().getContextoApuracao();
        Colaborador colaborador = apuracao.getColaborador();

        // Obtém Contexto Geral
        ContextoGeralRH geral = getContainer().getContextoGeral();

        if (apuracao.getColaborador().getCodigoVinculo() == 10) {
            int iDiaSem = geral.getDiaSem(apuracao.getData());
            boolean bSabadoSeguido = false;

            apuracao.setHorSit(400, apuracao.getHorSit(400, 401)); 
            apuracao.zeraHorasSituacao(401);

            bSabadoSeguido = Trabalhou2Sabados(apuracao, apuracao.getData().minusDays(7));
            if ((iDiaSem == 6) && (apuracao.getMarcacoesRealizadas(true).size() > 0)
                    && (apuracao.getData().getDayOfMonth() > 7) && (bSabadoSeguido)) {

                if ((bSabadoSeguido) && (apuracao.getData().getDayOfMonth() > 14)) {
                    bSabadoSeguido = Trabalhou2Sabados(apuracao, apuracao.getData().minusDays(14));
                }

                if (bSabadoSeguido) {
                    if (apuracao.getHorSit(400) > 0) {
                        apuracao.setHorSit(301, apuracao.getHorSit(400));
                        apuracao.zeraHorasSituacao(400);
                    }
                }
            }
            
            if (apuracao.getHorSit(400) > 0) {
                limitarBancoHoras(colaborador, apuracao);
            }
            
            apuracaoAposUltimoSabado(colaborador, apuracao);
                
            // Implementado por Thiago Carvalho - TSA Consultoria
            MappedParamProvider paramProvider2 = new MappedParamProvider();
            paramProvider2.setParam("dDatFer", apuracao.getData());
            IR002FEC c002Fec2 = consulta(IR002FEC.class, "DatFer=:dDatFer", paramProvider2);
            if ((c002Fec2 != null) && (apuracao.getHorSit(321,322) >= 361)){
                if (apuracao.getHorSit(321) > 60){
                    apuracao.setHorSit(321, apuracao.getHorSit(321) - 60);
                }
                else
                    apuracao.setHorSit(321, apuracao.getHorSit(322) - 60);                
            }                
            
            if ((iDiaSem == 6) && (apuracao.getHorSit(301, 400) >= 361)) {
                if ((apuracao.getHorSit(301) - 60) > 0) {
                    apuracao.setHorSit(301, apuracao.getHorSit(301) - 60);
                }
            } else if ((iDiaSem == 7) && (apuracao.getHorSit(321, 322) >= 361)) {
                if (apuracao.getHorSit(321) > 60) {
                    apuracao.setHorSit(321, apuracao.getHorSit(321) - 60);
                }
                else
                    if ((apuracao.getHorSit(322) - 60) > 0) {
                        apuracao.setHorSit(321, apuracao.getHorSit(322) - 60);
                    }                                        
            } else {
                MappedParamProvider paramProvider = new MappedParamProvider();
                paramProvider.setParam("dDatFer", apuracao.getData());
                IR002FEC c002Fec = consulta(IR002FEC.class, "DatFer=:dDatFer", paramProvider);
                if ((c002Fec != null) && (apuracao.getHorSit(321,322) >= 361)) {
                    if (apuracao.getHorSit(321) > 60) {
                        apuracao.setHorSit(321, apuracao.getHorSit(321) - 60);
                    }
                    else
                        if ((apuracao.getHorSit(322) - 60) > 0) {
                            apuracao.setHorSit(321, apuracao.getHorSit(322) - 60);
                        }
                }
            }
        }
        else {            
            if (apuracao.getColaborador().getCodigoVinculo() != 55) 
                apuracao.zeraHorasSituacao(301, 400);
            else
                apuracao.zeraHorasSituacao(400);
        }
    }

    private void apuracaoAposUltimoSabado(Colaborador colaborador, ContextoApuracao apuracao) {
        LocalDate dIniRef = LocalDate.now();
        LocalDate dDatPro = LocalDate.now();
        LocalDate dFimCmp = LocalDate.now();
        
        dDatPro = apuracao.getData();
        dFimCmp = apuracao.getDataFinal();
        dFimCmp = getContainer().getContextoGeral().getUltimoDiaMes(dFimCmp);
        int nDiaSem = getContainer().getContextoGeral().getDiaSem(dFimCmp);
        
        int sitRes = 0;
        
        if (nDiaSem == 7) /* Domingo */ {
            dIniRef = dFimCmp.minusDays(1);
        } else if (nDiaSem == 1) /* Segunda-Feira */ {
            dIniRef = dFimCmp.minusDays(2);
        } else if (nDiaSem == 2) /* Terça-Feira */ {
            dIniRef = dFimCmp.minusDays(3);
        } else if (nDiaSem == 3) /* Quarta-Feira */ {
            dIniRef = dFimCmp.minusDays(4);
        } else if (nDiaSem == 4) /* Quinta-Feira */ {
            dIniRef = dFimCmp.minusDays(5);
        } else if (nDiaSem == 5) /* Sexta-Feira */ {
            dIniRef = dFimCmp.minusDays(6);
        } else if (nDiaSem == 6) /* Sábado */ {
            dIniRef = dFimCmp;
        }
        
        if (dDatPro.isAfter(dIniRef)) {
            if (apuracao.getHorSit(400) > 0) {
                sitRes = 500;
                apuracao.setHorSit(sitRes, apuracao.getHorSit(400));
            }
            
            if (apuracao.getHorSit(450) > 0) {
                sitRes = 501;
                apuracao.setHorSit(sitRes, apuracao.getHorSit(450));
            }
        }
    }

    private void limitarBancoHoras(Colaborador colaborador, ContextoApuracao apuracao) {
        int nQtdBhr = 0;
        int nTotBhr = 0;
        int nBhrExc = 0;
        int nBhrApu = 0;
        int nDiaSem = 0;
        int nNumEmp = 0;
        int nTipCol = 0;
        int nNumCad = 0;
        
        LocalDate dIniSem = LocalDate.now();
        LocalDate dDatPro = LocalDate.now();
        
        nNumEmp = apuracao.getColaborador().getNumEmp();
        nTipCol = apuracao.getColaborador().getTipCol();
        nNumCad = apuracao.getColaborador().getNumCad();
        dDatPro = apuracao.getData();
        nDiaSem = getContainer().getContextoGeral().getDiaSem(dDatPro);
        
        if (nDiaSem == 7) /* Domingo */{
            dIniSem = dDatPro;
        } else if (nDiaSem == 1) /* Segunda-Feira */{
            dIniSem = dDatPro.minusDays(1);
        } else if (nDiaSem == 2) /* Terça-Feira */{
            dIniSem = dDatPro.minusDays(2);
        } else if (nDiaSem == 3) /* Quarta-Feira */{
            dIniSem = dDatPro.minusDays(3);
        } else if (nDiaSem == 4) /* Quinta-Feira */{
            dIniSem = dDatPro.minusDays(4);
        } else if (nDiaSem == 5) /* Sexta-Feira */{
            dIniSem = dDatPro.minusDays(5);
        } else if (nDiaSem == 6) /* Sábado */{
            dIniSem = dDatPro.minusDays(6);
        }
        
        MappedParamProvider paramsR066 = new MappedParamProvider();
        paramsR066.setParam("numEmp", nNumEmp);
        paramsR066.setParam("tipCol", nTipCol);
        paramsR066.setParam("numCad", nNumCad);
        paramsR066.setParam("iniSem", dIniSem);
        paramsR066.setParam("datPro", dDatPro.minusDays(1));
        
        ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
        cCursor_R066.addFilter("NumEmp = :numEmp and TipCol = :tipCol and NumCad = :numCad and DatApu >= :iniSem and DatApu <= :datPro and CodSit in (400)", paramsR066);
        cCursor_R066.open();
        try {
            while (cCursor_R066.next()) {
                IR066SIT eR066SIT = cCursor_R066.read();
                nQtdBhr = nQtdBhr + eR066SIT.getQtdHor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cCursor_R066.close();
        }
                
        int TotBhDia = 0;        
        TotBhDia = nQtdBhr + apuracao.getHorSit(400);
        
        if (nQtdBhr >= 240){
            apuracao.setHorSit(301,apuracao.getHorSit(400));
            apuracao.zeraHorasSituacao(400);
        } 
        
        if ((nQtdBhr < 240) && (TotBhDia >= 240)){
            int iAux = 240 - nQtdBhr;            
            apuracao.setHorSit(301, apuracao.getHorSit(400) - iAux);
            apuracao.setHorSit(400, iAux);            
        } 
    }

    public boolean Trabalhou2Sabados(ContextoApuracao apuracao, LocalDate data) {
        boolean bTrabalhou = false;

        MappedParamProvider paramsR070 = new MappedParamProvider();
        paramsR070.setParam("numEmp", apuracao.getColaborador().getNumEmp());
        paramsR070.setParam("tipCol", apuracao.getColaborador().getTipCol());
        paramsR070.setParam("numCad", apuracao.getColaborador().getNumCad());
        paramsR070.setParam("datAcc", apuracao.getData().minusDays(7));

        try {
            ICursor<IR070ACC> cCursor_R070 = getContainer().getEntitySession().newCursor(IR070ACC.class);
            cCursor_R070.addFilter(" NumEmp = :numEmp AND TipCol = :tipCol AND NumCad = :numCad AND DatApu = :datAcc ",
                    paramsR070);
            try {
                cCursor_R070.open();
                if (cCursor_R070.next()) {
                    bTrabalhou = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cCursor_R070.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bTrabalhou;
    }

    private <C extends IEntity> C consulta(Class<C> entidade, String filtro, MappedParamProvider paramProvider) {
        ICursor<C> cursor = getContainer().getEntitySession().newCursor(entidade);
        if (filtro != "")
            cursor.addFilter(filtro, paramProvider);
        cursor.open();
        try {
            if (cursor.first()) {
                C ler = cursor.read();
                return ler;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            cursor.close();
        }
        return null;
    }
}