package custom.android.apuracao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.entities.readonly.IR002FEC;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.entities.readonly.IR070ACC;
import com.senior.rule.Rule;

import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;

@Rule(description = "Regra de Apuração - ANDROID")
public class RegraApuracaoAndroid extends Apuracao {

    @Override
    public void execute() {
        // Obtém contexto da apuração
        ContextoApuracao apuracao = getContainer().getContextoApuracao();

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
                    apuracao.setHorSit(301, apuracao.getHorSit(400));
                    apuracao.zeraHorasSituacao(400);
                }

            } else if (((apuracao.getHorSit(400) > 0) || (apuracao.getHorSit(301) > 0))) {

                LocalDate dDatIni = apuracao.getData();
                int iBanHorSem = 0;

                if (iDiaSem == 1) // Segunda
                    dDatIni = apuracao.getData();
                else if (iDiaSem == 2)// Terca
                    dDatIni = apuracao.getData().minusDays(1);
                else if (iDiaSem == 3)// Quarta
                    dDatIni = apuracao.getData().minusDays(2);
                else if (iDiaSem == 4)// Quinta
                    dDatIni = apuracao.getData().minusDays(3);
                else if (iDiaSem == 5)// Sexta
                    dDatIni = apuracao.getData().minusDays(4);
                else if (iDiaSem == 6)// Sabado
                    dDatIni = apuracao.getData().minusDays(5);

                // Parâmetros do cursor R066Sit
                MappedParamProvider paramsR066 = new MappedParamProvider();
                paramsR066.setParam("numEmp", apuracao.getColaborador().getNumEmp());
                paramsR066.setParam("tipCol", apuracao.getColaborador().getTipCol());
                paramsR066.setParam("numCad", apuracao.getColaborador().getNumCad());
                paramsR066.setParam("datIni", dDatIni);
                paramsR066.setParam("datPro", apuracao.getData());

                // CURSOR R066Sit para buscar as Horas Extras da Semana
                try {
                    ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
                    cCursor_R066.addFilter(
                            "NumEmp = :numEmp AND " + "TipCol = :tipCol AND " + "NumCad = :numCad AND "
                                    + "CodSit in (301, 400) AND " + "DatApu >= :datIni AND " + "DatApu < :datPro",
                            paramsR066);
                    try {
                        cCursor_R066.open();
                        while (cCursor_R066.next()) {
                            IR066SIT eR066Sit = cCursor_R066.read();
                            iBanHorSem = iBanHorSem + eR066Sit.getQtdHor();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        cCursor_R066.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (iBanHorSem >= 240) {
                    apuracao.setHorSit(301, apuracao.getHorSit(400));
                    apuracao.zeraHorasSituacao(400);
                } else if (((iBanHorSem + apuracao.getHorSit(301, 400)) >= 240)) {
                    int iAux = 240 - iBanHorSem;
                    apuracao.setHorSit(301, apuracao.getHorSit(301, 400) - iAux);
                    apuracao.setHorSit(400, iAux);
                }
            }
            
            // Implementado por Thiago Carvalho - TSA Consultoria
            // 18-12-2023
            // Caso a pessoa trabalhou mais de 6 horas no feriado, deve descontar
            // uma hora de intervalo 
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
            
            // Caso a pessoa trabalhou mais de 6 horas no sabado, deve descontar
            // uma hora de intervalo
            if ((iDiaSem == 6) && (apuracao.getHorSit(301, 400) >= 361)) {
                // Na situacao 301, sempre vai ter mais de uma hora, pq pode ter
                // no maximo 4 horas de banco e o restante para extra
                apuracao.setHorSit(301, apuracao.getHorSit(301) - 60);
            } else if ((iDiaSem == 7) && (apuracao.getHorSit(321, 322) >= 361)) {
                if (apuracao.getHorSit(321) > 60)
                    apuracao.setHorSit(321, apuracao.getHorSit(321) - 60);
                else
                    apuracao.setHorSit(321, apuracao.getHorSit(322) - 60);                    
            } else {
                MappedParamProvider paramProvider = new MappedParamProvider();
                paramProvider.setParam("dDatFer", apuracao.getData());
                IR002FEC c002Fec = consulta(IR002FEC.class, "DatFer=:dDatFer", paramProvider);
                if ((c002Fec != null) && (apuracao.getHorSit(321,322) >= 361)) {
                    if (apuracao.getHorSit(321) > 60)
                        apuracao.setHorSit(321, apuracao.getHorSit(321) - 60);
                    else
                        apuracao.setHorSit(321, apuracao.getHorSit(322) - 60);    
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

    public boolean Trabalhou2Sabados(ContextoApuracao apuracao, LocalDate data) {
        boolean bTrabalhou = false;

        // Parâmetros do cursor R066Sit
        MappedParamProvider paramsR070 = new MappedParamProvider();
        paramsR070.setParam("numEmp", apuracao.getColaborador().getNumEmp());
        paramsR070.setParam("tipCol", apuracao.getColaborador().getTipCol());
        paramsR070.setParam("numCad", apuracao.getColaborador().getNumCad());
        paramsR070.setParam("datAcc", apuracao.getData().minusDays(7));

        // CURSOR R070Acc para buscar as Horas Extras da Semana
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

    // Método Genérico para fazer um cursor
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