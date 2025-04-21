package custom.senior.apuracao;

import org.joda.time.LocalDate;

import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.entities.readonly.IR002FEC;
import com.senior.rh.entities.readonly.IR003DTF;
import com.senior.rh.entities.readonly.IR038APU;
import com.senior.rh.ponto.colaborador.Horario;
import com.senior.rule.Rule;

import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;

@Rule(description = "Regra de Apuração")
public class RegraApuracao extends Apuracao {
	
	@Override
    public void execute() {
        
        // Busca os contextos
        ContextoApuracao apuracao = getContainer().getContextoApuracao();
        Colaborador colaborador = apuracao.getColaborador();
        ContextoGeralRH geral = getContainer().getContextoGeral();
        Horario horario = apuracao.getHorario();
        
        // Busca dados do colaborador e dia em processamento
        int numEmp = colaborador.getNumEmp();
        int tipCol = colaborador.getTipCol();
        int numCad = colaborador.getNumCad();        
        LocalDate datPro = apuracao.getData();
		LocalDate iniApu = apuracao.getDataInicial();
		LocalDate fimApu = apuracao.getDataFinal();
        int codHor = horario.getCodigo();

        // Inicialização variáveis
        MappedParamProvider paramProvider = new MappedParamProvider();
        int horasExtrasMes = 0;
        int diaFer = 0;
		int diaFac = 0;
		int horExt = 0;

		
		// Cursor para buscar a definição de situação mais recente do colaborador na tabela R038APU
		MappedParamProvider paramsR038APU = new MappedParamProvider();
		paramsR038APU.setParam("numEmp", numEmp);
		paramsR038APU.setParam("tipCol", tipCol);
		paramsR038APU.setParam("numCad", numCad);

		// Busca a definição de situação mais recente (maior data de IniApu)
		ICursor<IR038APU> cCursor_R038APU = getContainer().getEntitySession().newCursor(IR038APU.class);
		cCursor_R038APU.addFilter("NumEmp = :numEmp AND " +
		                          "TipCol = :tipCol AND " +
		                          "NumCad = :numCad "     +
		                          "ORDER BY IniApu DESC", paramsR038APU);

		int definicaoSituacao = 0; // Inicializa a definição de situação como 0

		try {
		    cCursor_R038APU.open();
		    if (cCursor_R038APU.next()) {
		        IR038APU eR038APU = cCursor_R038APU.read();
		        definicaoSituacao = eR038APU.getCodDSi(); // Obtém a definição de situação
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		} finally {
		    cCursor_R038APU.close();
		}

		
        // Verifica se o colaborador está na Definição de Situação 423 ou 442
        boolean isSituacao423 = (definicaoSituacao == 423);
        boolean isSituacao442 = (definicaoSituacao == 442);

        // Escalonamento Mensal (Situação 423)
        if (isSituacao423) {
            // Parâmetros do cursor R066Sit para horas extras no mês
            MappedParamProvider paramsR066Mes = new MappedParamProvider();
            paramsR066Mes.setParam("numEmp", numEmp);
            paramsR066Mes.setParam("tipCol", tipCol);
            paramsR066Mes.setParam("numCad", numCad);
            paramsR066Mes.setParam("iniApu", iniApu);
            paramsR066Mes.setParam("datPro", datPro);

            // CURSOR R066Sit para buscar as horas extras acumuladas no mês
            try {
                ICursor<IR066SIT> cCursor_R066Mes = getContainer().getEntitySession().newCursor(IR066SIT.class);
                cCursor_R066Mes.addFilter("NumEmp = :numEmp AND " + 
                                          "TipCol = :tipCol AND " + 
                                          "NumCad = :numCad AND " + 
                                          "CodSit = 400 AND " + 
                                          "DatApu >= :iniApu AND " + 
                                          "DatApu <= :datPro", paramsR066Mes);
                try {
                    cCursor_R066Mes.open();
                    while (cCursor_R066Mes.next()) {
                        IR066SIT eR066Sit = cCursor_R066Mes.read();
                        horasExtrasMes += eR066Sit.getQtdHor();
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cCursor_R066Mes.close();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Verificação do limite de horas extras no mês
            if (horasExtrasMes > 30) {
                int horasExcedentesMes = horasExtrasMes - 30;
                apuracao.setHorSit(400, 30);                 // Limite de 30 horas
                apuracao.setHorSit(404, horasExcedentesMes); // Horas excedentes
            }
        }

        // Escalonamento Semanal (Situação 442) - Definição da Data Inicial de acordo com o dia da Semana
        if (isSituacao442) {
            LocalDate datIni = LocalDate.parse("1900-12-31");
            
            if (geral.getDiaSem(datPro) == 2) { 	 // Terça-feira
                datIni = datPro.minusDays(1);
            } 
            else if (geral.getDiaSem(datPro) == 3) { // Quarta-feira
                datIni = datPro.minusDays(2);
            } 
            else if (geral.getDiaSem(datPro) == 4) { // Quinta-feira
                datIni = datPro.minusDays(3);
            } 
            else if (geral.getDiaSem(datPro) == 5) { // Sexta-feira
                datIni = datPro.minusDays(4);
            } 
            else if (geral.getDiaSem(datPro) == 6) { // Sábado
                datIni = datPro.minusDays(5);
            } 

            // Cursor para buscar as Horas Extras da Semana
            if (geral.getDiaSem(datPro) > 1) {
                MappedParamProvider paramsR066 = new MappedParamProvider();
                paramsR066.setParam("numEmp", numEmp);
                paramsR066.setParam("tipCol", tipCol);
                paramsR066.setParam("numCad", numCad);	
                paramsR066.setParam("datIni", datIni);
                paramsR066.setParam("datPro", datPro);									
                
                try {
                    ICursor<IR066SIT> cCursor_R066 = getContainer().getEntitySession().newCursor(IR066SIT.class);
                    cCursor_R066.addFilter("NumEmp = :numEmp AND "
                                         + "TipCol = :tipCol AND "
                                         + "NumCad = :numCad AND "
                                         + "CodSit in (301,302,307,308) AND "
                                         + "DatApu >= :datIni AND "
                                         + "DatApu < :datPro", paramsR066);

                    try {
                        cCursor_R066.open();
                        while (cCursor_R066.next()) {
                            IR066SIT eR066Sit = cCursor_R066.read();										
                            horExt = horExt + eR066Sit.getQtdHor();						
                        }
                    
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        cCursor_R066.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Verificação do limite de horas extras na semana
            if (horExt > 10) {
                int horasExcedentesSemana = horExt - 10;
                apuracao.setHorSit(301, 10);                    // Limite de 10 horas (50%)
                apuracao.setHorSit(307, horasExcedentesSemana); // Horas excedentes (90%)
            }
        }

        // Verifica se é um feriado
        paramProvider.setParam("datPro", datPro);
        IR002FEC c002Fec = consulta(IR002FEC.class, "CodFer = 2 AND DatFer = :datPro", paramProvider);
        if (c002Fec != null) {
            diaFer = 1;
        }

        // Verifica se é um dia facultativo (DSR)
        paramProvider.setParam("datPro", datPro);
        IR003DTF c003Dtf = consulta(IR003DTF.class, "CodDFa = 1 AND DatDFa = :datPro", paramProvider);
        if (c003Dtf != null) {
            diaFac = 1;
        }

        // Verifica se é um dia compensado
        boolean isCompensado = (codHor == 9998);

        // Tratamento para DSR, folga, feriado ou compensado
        if (diaFer == 1 || diaFac == 1 || isCompensado) {
            int horasExtrasDia = apuracao.getHorSit(301) + apuracao.getHorSit(302);
            if (horasExtrasDia > 8) {
                int horasExcedentesDia = horasExtrasDia - 8;
                apuracao.setHorSit(301, 8); // 100%
                apuracao.setHorSit(313, horasExcedentesDia); // 150%
            }
        }
    }

    // Método genérico para fazer um cursor
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